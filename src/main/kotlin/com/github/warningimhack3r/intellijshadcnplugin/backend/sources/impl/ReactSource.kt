package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.ReactConfig
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.ImportsPackagesReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.JSXClassReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.ReactDirectiveRemovalVisitor
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
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

    override fun adaptFileToConfig(file: PsiFile) {
        val config = getLocalConfig()

        val importsPackagesReplacementVisitor = ImportsPackagesReplacementVisitor(project)
        runReadAction { file.accept(importsPackagesReplacementVisitor) }
        importsPackagesReplacementVisitor.replaceImports visitor@{ `package` ->
            if (`package`.startsWith("@/registry/")) {
                return@visitor if (config.aliases.ui != null) {
                    `package`.replace(Regex("^@/registry/[^/]+/ui"), config.aliases.ui)
                } else {
                    `package`.replace(
                        Regex("^@/registry/[^/]+"),
                        config.aliases.components,
                    )
                }
            } else if (`package` == "@/lib/utils") {
                return@visitor config.aliases.utils
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
}
