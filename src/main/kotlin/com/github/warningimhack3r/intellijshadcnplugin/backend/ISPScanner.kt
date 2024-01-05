package com.github.warningimhack3r.intellijshadcnplugin.backend

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.ISPSource
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl.ISPReactSource
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl.ISPSvelteSource
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl.ISPVueSource
import com.intellij.openapi.project.Project

object ISPScanner {

    fun findShadcnImplementation(project: Project): ISPSource? {
        // TODO: Add other sources
        return FileManager(project).getVirtualFilesByName("components.json").firstOrNull()?.let { componentsJson ->
            val contents = componentsJson.contentsToByteArray().decodeToString()
            when {
                contents.contains("shadcn-svelte.com") -> ISPSvelteSource(project)
                contents.contains("ui.shadcn.com") -> ISPReactSource(project)
                contents.contains("shadcn-vue.com") -> ISPVueSource(project)
                else -> null
            }
        }
    }
}
