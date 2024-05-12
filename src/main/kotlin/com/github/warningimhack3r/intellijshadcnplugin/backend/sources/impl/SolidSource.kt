package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.PsiHelper
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SolidConfig
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.replacement.JSXClassReplacementVisitor
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
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

    override fun adaptFileToConfig(file: PsiFile) {
        val config = getLocalConfig()
        // Note: this does not prevent additional imports other than "cn" from being replaced,
        // but I'm following what the original code does for parity
        // (https://github.com/hngngn/shadcn-solid/blob/b808e0ecc9fd4689572d9fc0dfb7af81606a11f2/packages/cli/src/utils/transformers/transform-import.ts#L20-L29).
        val newContents = Regex(".*\\{.*[ ,\n\t]+cn[ ,].*}.*\"(@/lib/cn).*").replace(
            file.text.replace(
                Regex("@/registry/[^/]+"), cleanAlias(config.aliases.components)
            )
        ) { it.groupValues[0].replace(it.groupValues[1], config.aliases.utils) }
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

            file.accept(JSXClassReplacementVisitor visitor@{ `class` ->
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
}
