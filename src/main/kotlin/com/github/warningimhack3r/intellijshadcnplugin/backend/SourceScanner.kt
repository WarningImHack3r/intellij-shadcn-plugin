package com.github.warningimhack3r.intellijshadcnplugin.backend

import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonObject
import com.github.warningimhack3r.intellijshadcnplugin.backend.extensions.asJsonPrimitive
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.serialization.json.Json

object SourceScanner {
    val log = logger<SourceScanner>()

    fun findShadcnImplementation(project: Project): Source<*>? {
        val fileManager = FileManager.getInstance(project)
        return fileManager.getFileContentsAtPath("components.json")?.let { componentsJson ->
            val contents = Json.parseToJsonElement(componentsJson).asJsonObject
            val schema = contents?.get($$"$schema")?.asJsonPrimitive?.content ?: ""
            when {
                schema.contains("shadcn-svelte.com") -> SvelteSource(project)
                schema.contains("ui.shadcn.com") -> ReactSource(project)
                schema.contains("shadcn-vue.com") || contents?.keys?.contains("framework") == true -> VueSource(project)
                schema.contains("shadcn-solid") || schema.contains("solid-ui.com") -> SolidSource(project)
                else -> {
                    log.warn("Found a shadcn implementation but cannot figure out which, unrecognized schema: $schema")
                    null
                }
            }
        } ?: fileManager.getFileContentsAtPath("ui.config.json")?.let {
            SolidUISource(project)
        }.also {
            if (it == null) log.warn("No shadcn implementation found")
            else log.info("Found shadcn implementation: ${it.javaClass.name}")
        }
    }
}
