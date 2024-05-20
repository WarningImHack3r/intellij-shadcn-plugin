package com.github.warningimhack3r.intellijshadcnplugin.backend

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object SourceScanner {
    val log = logger<SourceScanner>()

    fun findShadcnImplementation(project: Project): Source<*>? {
        val fileManager = FileManager(project)
        return fileManager.getFileContentsAtPath("components.json")?.let { componentsJson ->
            val contents = Json.parseToJsonElement(componentsJson).jsonObject
            val schema = contents["\$schema"]?.jsonPrimitive?.content ?: ""
            when {
                schema.contains("shadcn-svelte.com") -> SvelteSource(project)
                schema.contains("ui.shadcn.com") -> ReactSource(project)
                schema.contains("shadcn-vue.com") || contents.keys.contains("framework") -> VueSource(project)
                schema.contains("shadcn-solid") || schema.contains("solid-ui.com") -> SolidSource(project)
                else -> null
            }
        } ?: fileManager.getFileContentsAtPath("ui.config.json")?.let {
            SolidUISource(project)
        }.also {
            if (it == null) log.warn("No shadcn implementation found")
            else log.info("Found shadcn implementation: ${it.javaClass.name}")
        }
    }
}
