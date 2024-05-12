package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.PsiHelper
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.VueConfig
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.remote.ComponentWithContents
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.VueClassReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.NoSuchFileException

class VueSource(project: Project) : Source<VueConfig>(project, VueConfig.serializer()) {
    companion object {
        private val log = logger<VueSource>()
    }

    override var framework = "Vue"

    override fun usesDirectoriesForComponents() = true

    override fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@")) return alias.also {
            log.debug("Alias $alias does not start with $ or @, returning it as-is")
        }

        fun resolvePath(configFile: String): String? {
            return Json.parseToJsonElement(configFile
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
        return if (!getLocalConfig().typescript) {
            extension.replace(
                Regex("\\.ts$"),
                ".js"
            )
        } else extension
    }

    override fun adaptFileToConfig(file: PsiFile) {
        val config = getLocalConfig()
        // Note: this does not prevent additional imports other than "cn" from being replaced,
        // but I'm following what the original code does for parity
        // (https://github.com/radix-vue/shadcn-vue/blob/9d9a6f929ce0f281b4af36161af80ed2bbdc4a16/packages/cli/src/utils/transformers/transform-import.ts#L19-L29).
        val newContents = Regex(".*\\{.*[ ,\n\t]+cn[ ,].*}.*\"(@/lib/cn).*").replace(
            file.text.replace(
                Regex("@/registry/[^/]+"), cleanAlias(config.aliases.components)
            )
        ) { result ->
            result.groupValues[0].replace(result.groupValues[1], cleanAlias(config.aliases.utils))
        }

        if (!config.typescript) {
            NotificationManager(project).sendNotification(
                "TypeScript option for Vue",
                "You have TypeScript disabled in your shadcn/ui config. This feature is not supported yet. Please install/update your components with the CLI for now.",
                NotificationType.WARNING
            )
            // TODO: detype Vue file
        }
        PsiHelper.writeAction(file, "Replacing imports") {
            file.replace(PsiHelper.createPsiFile(project, file.fileType, newContents))
        }

        if (!config.tailwind.cssVariables) {
            val prefixesToReplace = listOf("bg-", "text-", "border-", "ring-offset-", "ring-")

            val inlineColors = fetchColors().jsonObject["inlineColors"]?.jsonObject
                ?: throw Exception("Inline colors not found")
            val lightColors = inlineColors.jsonObject["light"]?.jsonObject?.let { lightColors ->
                lightColors.keys.associateWith { lightColors[it]?.jsonPrimitive?.content ?: "" }
            } ?: emptyMap()
            val darkColors = inlineColors.jsonObject["dark"]?.jsonObject?.let { darkColors ->
                darkColors.keys.associateWith { darkColors[it]?.jsonPrimitive?.content ?: "" }
            } ?: emptyMap()

            file.accept(VueClassReplacementVisitor visitor@{ `class` ->
                val modifier = if (`class`.contains(":")) `class`.substringBeforeLast(":") + ":" else ""
                val className = `class`.substringAfterLast(":")
                if (className == "border") {
                    return@visitor "${modifier}border ${modifier}border-border"
                }
                val prefix = prefixesToReplace.find { className.startsWith(it) }
                    ?: return@visitor "$modifier$className"
                val color = className.substringAfter(prefix)
                val lightColor = lightColors[color]
                val darkColor = darkColors[color]
                if (lightColor != null && darkColor != null) {
                    "$modifier$prefix$lightColor dark:$modifier$prefix$darkColor"
                } else "$modifier$className"
            })
        }
    }

    override fun getRegistryDependencies(component: ComponentWithContents): List<ComponentWithContents> {
        return super.getRegistryDependencies(component.copy(
            registryDependencies = component.registryDependencies.filterNot { it == "utils" }
        ))
    }
}
