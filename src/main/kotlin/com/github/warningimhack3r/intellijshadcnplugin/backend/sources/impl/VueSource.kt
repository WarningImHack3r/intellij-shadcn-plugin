package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.VueConfig
import com.github.warningimhack3r.intellijshadcnplugin.notifications.NotificationManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.util.applyIf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.NoSuchFileException

class VueSource(project: Project) : Source<VueConfig>(project, VueConfig.serializer()) {
    override var framework = "Vue"

    override fun adaptFileExtensionToConfig(extension: String): String {
        return if (!getLocalConfig().typescript) {
            extension.replace(
                Regex("\\.ts$"),
                ".js"
            )
        } else extension
    }

    override fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@")) return alias
        val tsConfig = FileManager(project).getFileContentsAtPath("tsconfig.json") ?: throw NoSuchFileException("tsconfig.json not found")
        val aliasPath = Json.parseToJsonElement(tsConfig)
            .jsonObject["compilerOptions"]
            ?.jsonObject?.get("paths")
            ?.jsonObject?.get("${alias.substringBefore("/")}/*")
            ?.jsonArray?.get(0)
            ?.jsonPrimitive?.content ?: throw Exception("Cannot find alias $alias")
        return aliasPath.replace(Regex("^\\./"), "")
            .replace(Regex("\\*$"), alias.substringAfter("/"))
    }

    override fun adaptFileToConfig(contents: String): String {
        val config = getLocalConfig()
        val newContents = contents.replace(
            Regex("@/lib/registry/[^/]+"), cleanAlias(config.aliases.components)
        ).replace(
            // Note: this does not prevent additional imports other than "cn" from being replaced,
            // but I'm following what the original code does for parity
            // (https://github.com/radix-vue/shadcn-vue/blob/9d9a6f929ce0f281b4af36161af80ed2bbdc4a16/packages/cli/src/utils/transformers/transform-import.ts#L19-L29).
            Regex(".*\\{.*[ ,\n\t]+cn[ ,].*}.*\"@/lib/utils"),
            cleanAlias(config.aliases.utils)
        ).applyIf(!config.typescript) {
            NotificationManager(project).sendNotification(
                "TypeScript option for Vue",
                "You have TypeScript disabled in your shadcn/ui config. This feature is not supported yet. Please install/update your components with the CLI for now.",
                NotificationType.WARNING
            )
            // TODO: detype Vue file
            this
        }
        return if (!config.tailwind.cssVariables) {
            /**
             * Converts CSS variables to Tailwind utility classes.
             * @param classes The classes to convert, an unquoted string of space-separated class names.
             * @param lightColors The light colors map to use.
             * @param darkColors The dark colors map to use.
             * @return The converted classes.
             */
            fun variablesToUtilities(classes: String, lightColors: Map<String, String>, darkColors: Map<String, String>): String {
                // Note: this does not include `border` classes at the beginning or end of the string,
                // but I'm once again following what the original code does for parity
                // (https://github.com/radix-vue/shadcn-vue/blob/4214134e1834fdabcc5f0354e11593360f076e8d/packages/cli/src/utils/transformers/transform-css-vars.ts#L87-L89).
                val newClasses = classes.replace(" border ", " border border-border ")

                val prefixesToReplace = listOf("bg-", "text-", "border-", "ring-offset-", "ring-")

                /**
                 * Replaces a class with CSS variables with Tailwind utility classes.
                 * @param class The class to replace.
                 * @return The replaced class.
                 */
                fun replaceClass(`class`: String): String {
                    val prefix = prefixesToReplace.find { `class`.startsWith(it) } ?: return `class`
                    val color = `class`.substringAfter(prefix)
                    val lightColor = lightColors[color]
                    val darkColor = darkColors[color]
                    return if (lightColor != null && darkColor != null) {
                        "$prefix$lightColor dark:$prefix$darkColor"
                    } else `class`
                }

                return newClasses
                    .split(" ")
                    .filterNot { it.isEmpty() }
                    .joinToString(" ") {
                        val className = it.trim().split(":")
                        if (className.size == 1) {
                            replaceClass(className[0])
                        } else {
                            "${className.dropLast(1).joinToString(":")}:${replaceClass(className.last())}"
                        }
                    }
            }

            fun handleClasses(classes: String): String {
                val inlineColors = fetchColors().jsonObject["inlineColors"]?.jsonObject ?: throw Exception("Inline colors not found")
                return variablesToUtilities(
                    classes,
                    inlineColors.jsonObject["light"]?.jsonObject?.let { lightColors ->
                        lightColors.keys.associateWith { lightColors[it]?.jsonPrimitive?.content ?: "" }
                    } ?: emptyMap(),
                    inlineColors.jsonObject["dark"]?.jsonObject?.let { darkColors ->
                        darkColors.keys.associateWith { darkColors[it]?.jsonPrimitive?.content ?: "" }
                    } ?: emptyMap()
                )
            }

            val notTemplateClasses = Regex("[^:]class=(?:(?!>)[^\"'])*[\"']([^\"']*)[\"']").replace(newContents) { result ->
                result.groupValues[0].replace(
                    result.groupValues[1],
                    handleClasses(result.groupValues[1])
                )
            }
            // Double quoted templates
            Regex(":class=(?:(?!>)[^\"])*\"([^\"]*)\"").replace(notTemplateClasses) { result ->
                val group = result.groupValues[1]
                result.groupValues[0].replace(
                    group,
                    handleClasses(group
                        .replace("\n", " ")
                        .split(", ")
                        .map { it.trim() }
                        .last { it.startsWith("'") || it.endsWith("'") })
                )
            }
        } else newContents
    }
}
