package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.http.RequestSender
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.VueConfig
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote.ComponentWithContents
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.ImportsPackagesReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.VueClassReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.NoSuchFileException

open class VueSource(project: Project) : Source<VueConfig>(project, VueConfig.serializer()) {
    companion object {
        private val log = logger<VueSource>()
        private var isJsUnsupportedNotified = false
    }

    override var framework = "Vue"

    override fun getURLPathForComponent(componentName: String) =
        "registry/styles/${getLocalConfig().style}/$componentName.json"

    override fun getLocalPathForComponents() = getLocalConfig().aliases.components

    override fun usesDirectoriesForComponents() = true

    override fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@") && !alias.startsWith("~")) {
            log.warn("Alias $alias does not start with $, @ or ~, returning it as-is")
            return alias
        }

        fun resolvePath(configFile: String): String? {
            return tsConfigJson.parseToJsonElement(configFile
                .split("\n")
                .filterNot { it.trim().startsWith("//") } // remove comments
                .joinToString("\n")
            )
                .jsonObject["compilerOptions"]
                ?.jsonObject?.get("paths")
                ?.jsonObject?.get("${alias.substringBefore("/")}/*")
                ?.jsonArray?.get(0)
                ?.jsonPrimitive?.content
        }

        val config = getLocalConfig()
        val tsConfigLocation = when (config.framework) {
            VueConfig.Framework.NUXT -> ".nuxt/tsconfig.json"
            else -> "tsconfig.json"
        }.let { if (!config.typescript) "jsconfig.json" else it }

        val tsConfig = FileManager(project).getFileContentsAtPath(tsConfigLocation)
            ?: throw NoSuchFileException("$tsConfigLocation not found")
        val aliasPath = (resolvePath(tsConfig) ?: if (config.typescript) {
            resolvePath("tsconfig.app.json")
        } else null) ?: throw Exception("Cannot find alias $alias in $tsConfig")
        return aliasPath.replace(Regex("^\\.+/"), "")
            .replace(Regex("\\*$"), alias.substringAfter("/")).also {
                log.debug("Resolved alias $alias to $it")
            }
    }

    override fun adaptFileExtensionToConfig(extension: String): String {
        return if (getLocalConfig().typescript) extension else {
            extension.replace(
                Regex("\\.ts$"),
                ".js"
            )
        }
    }

    override fun adaptFileToConfig(file: PsiFile) {
        val config = getLocalConfig()
        if (!config.typescript) {
            if (!isJsUnsupportedNotified) {
                NotificationManager(project).sendNotification(
                    "TypeScript option for Vue",
                    "You have TypeScript disabled in your shadcn/ui config. This feature is not supported yet. Please install/update your components with the CLI for now.",
                    NotificationType.WARNING
                )
                isJsUnsupportedNotified = true
            }
            // TODO: detype Vue file
        }

        val importsPackagesReplacementVisitor = ImportsPackagesReplacementVisitor(project)
        runReadAction { file.accept(importsPackagesReplacementVisitor) }
        importsPackagesReplacementVisitor.replaceImports replacer@{ `package` ->
            if (`package`.startsWith("@/lib/registry/")) {
                return@replacer if (config.aliases.ui != null) {
                    `package`.replace(Regex("^@/lib/registry/[^/]+/ui"), escapeRegexValue(config.aliases.ui))
                } else {
                    `package`.replace(
                        Regex("^@/lib/registry/[^/]+"),
                        escapeRegexValue(config.aliases.components)
                    )
                }
            } else if (`package` == "@/lib/utils") {
                return@replacer config.aliases.utils
            }
            `package`
        }

        val prefixesToReplace = listOf("bg-", "text-", "border-", "ring-offset-", "ring-")

        val inlineColors = fetchColors().jsonObject["inlineColors"]?.jsonObject
            ?: throw Exception("Inline colors not found")
        val lightColors = inlineColors.jsonObject["light"]?.jsonObject?.let { lightColors ->
            lightColors.keys.associateWith { lightColors[it]?.jsonPrimitive?.content ?: "" }
        } ?: emptyMap()
        val darkColors = inlineColors.jsonObject["dark"]?.jsonObject?.let { darkColors ->
            darkColors.keys.associateWith { darkColors[it]?.jsonPrimitive?.content ?: "" }
        } ?: emptyMap()

        val classReplacementVisitor = VueClassReplacementVisitor(project)
        runReadAction { file.accept(classReplacementVisitor) }
        classReplacementVisitor.replaceClasses replacer@{ `class` ->
            val modifier = if (`class`.contains(":")) `class`.substringBeforeLast(":") + ":" else ""
            val className = `class`.substringAfterLast(":")
            val twPrefix = config.tailwind.prefix
            if (config.tailwind.cssVariables) {
                return@replacer "$modifier$twPrefix$className"
            }
            if (className == "border") {
                return@replacer "$modifier${twPrefix}border $modifier${twPrefix}border-border"
            }
            val prefix = prefixesToReplace.find { className.startsWith(it) }
                ?: return@replacer "$modifier$twPrefix$className"
            val color = className.substringAfter(prefix)
            val lightColor = lightColors[color]
            val darkColor = darkColors[color]
            if (lightColor != null && darkColor != null) {
                "$modifier$twPrefix$prefix$lightColor dark:$modifier$twPrefix$prefix$darkColor"
            } else "$modifier$twPrefix$className"
        }
    }

    override fun fetchColors(): JsonElement {
        val baseColor = getLocalConfig().tailwind.baseColor
        return RequestSender.sendRequest("$domain/registry/colors/$baseColor.json").ok {
            Json.parseToJsonElement(it.body)
        } ?: throw Exception("Colors not found")
    }

    override fun getRegistryDependencies(component: ComponentWithContents): List<ComponentWithContents> {
        return super.getRegistryDependencies(component.copy(
            registryDependencies = component.registryDependencies.filterNot { it == "utils" }
        ))
    }
}
