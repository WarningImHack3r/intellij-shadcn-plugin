package com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.ShellRunner
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.config.SvelteConfig
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.NoSuchFileException

class SvelteSource(project: Project) : Source<SvelteConfig>(project, SvelteConfig.serializer()) {
    override var framework = "Svelte"

    override fun usesDirectoriesForComponents() = true

    override fun resolveAlias(alias: String): String {
        if (!alias.startsWith("$") && !alias.startsWith("@")) return alias
        var tsConfig = FileManager(project).getFileContentsAtPath(".svelte-kit/tsconfig.json")
        if (tsConfig == null) {
            ShellRunner(project).execute(arrayOf("npx", "svelte-kit", "sync"))
            Thread.sleep(250) // wait for the sync to create the files
            tsConfig = FileManager(project).getFileContentsAtPath(".svelte-kit/tsconfig.json") ?: throw NoSuchFileException("Cannot get or generate .svelte-kit/tsconfig.json")
        }
        val aliasPath = Json.parseToJsonElement(tsConfig)
            .jsonObject["compilerOptions"]
            ?.jsonObject?.get("paths")
            ?.jsonObject?.get(alias.substringBefore("/"))
            ?.jsonArray?.get(0)
            ?.jsonPrimitive?.content ?: throw Exception("Cannot find alias $alias")
        return "${aliasPath.replace(Regex("^\\.+/"), "")}/${alias.substringAfter("/")}"
    }

    override fun adaptFileToConfig(contents: String): String {
        val config = getLocalConfig()
        return contents.replace(
            Regex("@/registry/[^/]+"), cleanAlias(config.aliases.components)
        ).replace(
            "\$lib/utils", config.aliases.utils
        )
    }
}
