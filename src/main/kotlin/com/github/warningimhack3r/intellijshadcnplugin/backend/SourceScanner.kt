package com.github.warningimhack3r.intellijshadcnplugin.backend

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl.ReactSource
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl.SolidSource
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl.SvelteSource
import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.impl.VueSource
import com.intellij.openapi.project.Project

object SourceScanner {

    fun findShadcnImplementation(project: Project): Source<*>? {
        return FileManager(project).getVirtualFilesByName("components.json").firstOrNull()?.let { componentsJson ->
            val contents = componentsJson.contentsToByteArray().decodeToString()
            when {
                contents.contains("shadcn-svelte.com") -> SvelteSource(project)
                contents.contains("ui.shadcn.com") -> ReactSource(project)
                contents.contains("shadcn-vue.com")
                        || contents.contains("\"framework\": \"") -> VueSource(project)
                contents.contains("shadcn-solid") -> SolidSource(project)
                else -> null
            }
        }
    }
}
