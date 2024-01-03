package com.github.warningimhack3r.intellijshadcnplugin.backend

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.ISPSource
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.ISPSvelteSource
import com.intellij.openapi.project.Project

object ISPScanner {

    fun findShadcnImplementation(project: Project): ISPSource? {
        FileManager(project).getVirtualFilesByName("components.json").firstOrNull()?.let { componentsJson ->
            val contents = componentsJson.contentsToByteArray().decodeToString()
            if (contents.contains("shadcn-svelte.com")) {
                return ISPSvelteSource(project)
            }
        }
        // TODO: Add other sources
        return null
    }
}
