package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.ReactConfig
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.applyIf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.NoSuchFileException

class ReactSource(project: Project) : Source<ReactConfig>(project, ReactConfig.serializer()) {
    companion object {
        private val log = logger<ReactSource>()
    }

    override var framework = "React"

    override fun usesDirectoriesForComponents() = false

    override fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@")) return alias.also {
            log.debug("Alias $alias does not start with $ or @, returning it as-is")
        }
        val configFile = if (getLocalConfig().tsx) "tsconfig.json" else "jsconfig.json"
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

    override fun adaptFileExtensionToConfig(extension: String): String {
        return if (!getLocalConfig().tsx) {
            extension
                .replace(Regex("\\.tsx$"), ".ts")
                .replace(Regex("\\.jsx$"), ".js")
        } else extension
    }

    override fun adaptFileToConfig(contents: String): String {
        val config = getLocalConfig()
        // Note: this does not prevent additional imports other than "cn" from being replaced,
        // but I'm once again following what the original code does for parity
        // (https://github.com/shadcn-ui/ui/blob/fb614ac2921a84b916c56e9091aa0ae8e129c565/packages/cli/src/utils/transformers/transform-import.ts#L25-L35).
        val newContents = Regex(".*\\{.*[ ,\n\t]+cn[ ,].*}.*\"(@/lib/cn).*").replace(
            // Note: this condition does not replace UI paths (= $components/$ui) by the components path
            // if the UI alias is not set.
            // For me, this is a bug, but I'm following what the original code does for parity
            // (https://github.com/shadcn-ui/ui/blob/fb614ac2921a84b916c56e9091aa0ae8e129c565/packages/cli/src/utils/transformers/transform-import.ts#L10-L23).
            if (config.aliases.ui != null) {
                contents.replace(
                    Regex("@/registry/[^/]+/ui"), cleanAlias(config.aliases.ui)
                )
            } else contents.replace(
                Regex("@/registry/[^/]+"), cleanAlias(config.aliases.components)
            )
        ) { result ->
            result.groupValues[0].replace(result.groupValues[1], config.aliases.utils)
        }.applyIf(config.rsc) {
            replace(
                Regex("\"use client\";*\n"), ""
            )
        }

        /**
         * Prepends `tw-` to all Tailwind classes.
         * @param classes The classes to prefix, an unquoted string of space-separated class names.
         * @param prefix The prefix to add to each class name.
         * @return The prefixed classes.
         */
        fun prefixClasses(classes: String, prefix: String): String = classes
            .split(" ")
            .filterNot { it.isEmpty() }
            .joinToString(" ") {
                val className = it.trim().split(":")
                if (className.size == 1) {
                    "$prefix${className[0]}"
                } else {
                    "${className.dropLast(1).joinToString(":")}:$prefix${className.last()}"
                }
            }

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
            // (https://github.com/shadcn-ui/ui/blob/fb614ac2921a84b916c56e9091aa0ae8e129c565/packages/cli/src/utils/transformers/transform-css-vars.ts#L142-L145).
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
            var newClasses = classes
            if (!config.tailwind.cssVariables) {
                val inlineColors = fetchColors().jsonObject["inlineColors"]?.jsonObject
                    ?: throw Exception("Inline colors not found")
                newClasses = variablesToUtilities(
                    newClasses,
                    inlineColors.jsonObject["light"]?.jsonObject?.let { lightColors ->
                        lightColors.keys.associateWith { lightColors[it]?.jsonPrimitive?.content ?: "" }
                    } ?: emptyMap(),
                    inlineColors.jsonObject["dark"]?.jsonObject?.let { darkColors ->
                        darkColors.keys.associateWith { darkColors[it]?.jsonPrimitive?.content ?: "" }
                    } ?: emptyMap()
                )
            }
            if (config.tailwind.prefix.isNotEmpty()) {
                newClasses = prefixClasses(newClasses, config.tailwind.prefix)
            }
            return newClasses
        }

        return Regex("className=(?:(?!>)[^\"'])*[\"']([^>]*)[\"']").replace(newContents) { result ->
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
    }
}
