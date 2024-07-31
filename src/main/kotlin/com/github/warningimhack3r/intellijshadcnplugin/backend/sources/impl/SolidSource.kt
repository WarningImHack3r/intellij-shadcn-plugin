package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonArray
import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonObject
import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonPrimitive
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.RequestSender
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SolidConfig
import com.github.warningimhack3r.intellijshadcnplugin.backend.transformers.replacement.ImportsPackagesReplacementVisitor
import com.github.warningimhack3r.intellijshadcnplugin.backend.transformers.replacement.JSXClassReplacementVisitor
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.nio.file.NoSuchFileException

open class SolidSource(project: Project) : Source<SolidConfig>(project, SolidConfig.serializer()) {
    companion object {
        private val log = logger<SolidSource>()
    }

    override var framework = "Solid"

    override fun getURLPathForRoot() = "registry/index.json"

    private fun cssFrameworkName(): String {
        val config = getLocalConfig()
        return when {
            config.tailwind != null -> "tailwindcss"
            config.uno != null -> "unocss"
            else -> throw Exception("Framework not found. Is your config valid?")
        }
    }

    override fun getURLPathForComponent(componentName: String) =
        "registry/frameworks/${cssFrameworkName()}/$componentName.json"

    override fun usesDirectoriesForComponents() = false

    override fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@") && !alias.startsWith("~")) {
            log.warn("Alias $alias does not start with $, @ or ~, returning it as-is")
            return alias
        }
        val configFile = "tsconfig.json"
        val tsConfig = FileManager.getInstance(project).getFileContentsAtPath(configFile)
            ?: throw NoSuchFileException("$configFile not found")
        val aliasPath = parseTsConfig(tsConfig)
            .asJsonObject?.get("compilerOptions")
            ?.asJsonObject?.get("paths")
            ?.asJsonObject?.get("${alias.substringBefore("/")}/*")
            ?.asJsonArray?.get(0)
            ?.asJsonPrimitive?.content ?: throw Exception("Cannot find alias $alias in $tsConfig")
        return aliasPath.replace(Regex("^\\.+/"), "")
            .replace(Regex("\\*$"), alias.substringAfter("/")).also {
                log.debug("Resolved alias $alias to $it")
            }
    }

    override fun adaptFileToConfig(file: PsiFile) {
        val config = getLocalConfig()

        val importsPackagesReplacementVisitor = ImportsPackagesReplacementVisitor(project)
        runReadAction { file.accept(importsPackagesReplacementVisitor) }
        importsPackagesReplacementVisitor.replaceImports replacer@{ `package` ->
            if (`package` == "@/libs/cn") {
                return@replacer config.aliases.utils
            }
            `package`
        }

        val prefixesToReplace = listOf("bg-", "text-", "border-", "ring-offset-", "ring-")

        val inlineColors = fetchColors().asJsonObject?.get("inlineColors")?.asJsonObject
            ?: throw Exception("Inline colors not found")
        val lightColors = inlineColors["light"]?.asJsonObject?.let { lightColors ->
            lightColors.keys.associateWith { lightColors[it]?.asJsonPrimitive?.content ?: "" }
        } ?: emptyMap()
        val darkColors = inlineColors["dark"]?.asJsonObject?.let { darkColors ->
            darkColors.keys.associateWith { darkColors[it]?.asJsonPrimitive?.content ?: "" }
        } ?: emptyMap()

        val replacementVisitor = JSXClassReplacementVisitor(project)
        runReadAction { file.accept(replacementVisitor) }
        replacementVisitor.replaceClasses replacer@{ `class` ->
            val modifier = if (`class`.contains(":")) `class`.substringBeforeLast(":") + ":" else ""
            val className = `class`.substringAfterLast(":")
            val twPrefix = config.tailwind?.prefix ?: config.uno?.prefix ?: ""
            if (config.tailwind?.css?.variable == true || config.uno?.css?.variable == true) {
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
        val config = getLocalConfig()
        val baseColor = config.tailwind?.color ?: config.uno?.color
        ?: throw Exception("Base color not found. Is your config valid?")
        return RequestSender.sendRequest("$domain/registry/colors/${cssFrameworkName()}/$baseColor.json").ok {
            Json.parseToJsonElement(it.body)
        } ?: throw Exception("Colors not found")
    }
}
