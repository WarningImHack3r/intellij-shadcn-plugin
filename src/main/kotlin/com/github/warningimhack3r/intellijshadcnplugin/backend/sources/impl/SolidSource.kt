package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SolidConfig
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.NoSuchFileException

class SolidSource(project: Project) : Source<SolidConfig>(project, SolidConfig.serializer()) {
    companion object {
        private val log = logger<SolidSource>()
    }

    override var framework = "Solid"

    override fun usesDirectoriesForComponents() = false

    override fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@")) return alias.also {
            log.debug("Alias $alias does not start with $ or @, returning it as-is")
        }
        val configFile = "tsconfig.json"
        val tsConfig = FileManager(project).getFileContentsAtPath(configFile)
            ?: throw NoSuchFileException("$configFile not found")
        val aliasPath = Json.parseToJsonElement(tsConfig)
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

    override fun adaptFileToConfig(contents: String): String {
        val config = getLocalConfig()
        // Note: this does not prevent additional imports other than "cn" from being replaced,
        // but I'm following what the original code does for parity
        // (https://github.com/hngngn/shadcn-solid/blob/b808e0ecc9fd4689572d9fc0dfb7af81606a11f2/packages/cli/src/utils/transformers/transform-import.ts#L20-L29).
        val newContents = Regex(".*\\{.*[ ,\n\t]+cn[ ,].*}.*\"(@/lib/cn).*").replace(
            contents.replace(
                Regex("@/registry/[^/]+"), cleanAlias(config.aliases.components)
            )
        ) { it.groupValues[0].replace(it.groupValues[1], config.aliases.utils) }

        return if (!config.tailwind.cssVariables) {
            /**
             * Converts CSS variables to Tailwind utility classes.
             * @param classes The classes to convert, an unquoted string of space-separated class names.
             * @param lightColors The light colors map to use.
             * @param darkColors The dark colors map to use.
             * @return The converted classes.
             */
            fun variablesToUtilities(
                classes: String,
                lightColors: Map<String, String>,
                darkColors: Map<String, String>
            ): String {
                // Note: this does not include `border` classes at the beginning or end of the string,
                // but I'm once again following what the original code does for parity
                // (https://github.com/hngngn/shadcn-solid/blob/b808e0ecc9fd4689572d9fc0dfb7af81606a11f2/packages/cli/src/utils/transformers/transform-css-vars.ts#L144-L147).
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
                val inlineColors = fetchColors().jsonObject["inlineColors"]?.jsonObject
                    ?: throw Exception("Inline colors not found")
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

            Regex("className=(?:(?!>)[^\"'])*[\"']([^>]*)[\"']").replace(newContents) { result ->
                // matches any className, and takes everything inside the first quote to the last quote found before the closing `>`
                // if no quotes are found before the closing `>`, skips the match
                val match = result.groupValues[0]
                val group = result.groupValues[1]
                match.replace(
                    group,
                    // if the group contains a quote, we assume the classes are the last quoted string in the group
                    if (group.contains("\"")) {
                        group.substringBeforeLast('"') + "\"" + handleClasses(
                            group.substringAfterLast('"')
                        )
                    } else if (group.contains("'")) {
                        group.substringBeforeLast("'") + "'" + handleClasses(
                            group.substringAfterLast("'")
                        )
                    } else handleClasses(group)
                )
            }
        } else newContents
    }
}
