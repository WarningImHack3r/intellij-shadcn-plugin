package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.RequestSender
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.ReactConfig
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.ImportsPackagesReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.JSXClassReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.ReactDirectiveRemovalVisitor
import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.serialization.json.*
import java.nio.file.NoSuchFileException

open class ReactSource(project: Project) : Source<ReactConfig>(project, ReactConfig.serializer()) {
    companion object {
        private val log = logger<ReactSource>()
        private var isJsUnsupportedNotified = false
    }

    override var framework = "React"

    override fun getURLPathForComponent(componentName: String) =
        "r/styles/${getLocalConfig().style}/$componentName.json"

    override fun getLocalPathForComponents() = getLocalConfig().aliases.components

    override fun usesDirectoriesForComponents() = false

    override fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@") && !alias.startsWith("~")) {
            log.warn("Alias $alias does not start with $, @ or ~, returning it as-is")
            return alias
        }
        val configFile = if (getLocalConfig().tsx) "tsconfig.json" else "jsconfig.json"
        val tsConfig = FileManager.getInstance(project).getFileContentsAtPath(configFile)
            ?: throw NoSuchFileException("$configFile not found")
        val aliasPath = parseTsConfig(tsConfig)
            .jsonObject["compilerOptions"]
            ?.jsonObject?.get("paths")
            ?.jsonObject?.get("${alias.substringBefore("/")}/*")
            ?.jsonArray?.get(0)
            ?.jsonPrimitive?.content ?: throw Exception("Cannot find alias $alias in $tsConfig")
        return aliasPath.replace(Regex("^\\.+/"), "")
            .replace(Regex("\\*$"), alias.substringAfter("/")).also {
                log.debug("Resolved alias $alias to $it")
            }
    }

    override fun adaptFileExtensionToConfig(extension: String): String {
        return if (!getLocalConfig().tsx) {
            extension
                .replace(Regex("\\.tsx$"), ".ts")
                .replace(Regex("\\.jsx$"), ".js")
        } else extension
    }

    override fun adaptFileToConfig(file: PsiFile) {
        val config = getLocalConfig()

        if (!config.tsx) {
            if (!isJsUnsupportedNotified) {
                NotificationManager(project).sendNotification(
                    "TypeScript option for React",
                    "You have TypeScript disabled in your shadcn/ui config. This feature is not supported yet. Please install/update your components with the CLI for now.",
                    NotificationType.WARNING
                )
                isJsUnsupportedNotified = true
            }
            // TODO: detype React file
        }

        val importsPackagesReplacementVisitor = ImportsPackagesReplacementVisitor(project)
        runReadAction { file.accept(importsPackagesReplacementVisitor) }
        importsPackagesReplacementVisitor.replaceImports replacer@{ `package` ->
            if (`package`.startsWith("@/registry/")) {
                return@replacer if (config.aliases.ui != null) {
                    `package`.replace(Regex("^@/registry/[^/]+/ui"), escapeRegexValue(config.aliases.ui))
                } else {
                    `package`.replace(
                        Regex("^@/registry/[^/]+"),
                        escapeRegexValue(config.aliases.components)
                    )
                }
            } else if (`package` == "@/lib/utils") {
                return@replacer config.aliases.utils
            }
            `package`
        }

        if (!config.rsc) {
            val directiveVisitor = ReactDirectiveRemovalVisitor(project) { directive ->
                directive == "use client"
            }
            runReadAction { file.accept(directiveVisitor) }
            directiveVisitor.removeMatchingElements()
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

        val replacementVisitor = JSXClassReplacementVisitor(project)
        runReadAction { file.accept(replacementVisitor) }
        replacementVisitor.replaceClasses replacer@{ `class` ->
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
        return RequestSender.sendRequest("$domain/r/colors/$baseColor.json").ok {
            Json.parseToJsonElement(it.body)
        } ?: throw Exception("Colors not found")
    }
}
