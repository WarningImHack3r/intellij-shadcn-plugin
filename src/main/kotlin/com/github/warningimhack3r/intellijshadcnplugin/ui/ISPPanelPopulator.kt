package com.github.warningimhack3r.intellijshadcnplugin.ui

import com.github.warningimhack3r.intellijshadcnplugin.backend.SourceScanner
import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import javax.swing.JComponent
import javax.swing.SwingConstants

class ISPPanelPopulator(private val project: Project) {
    companion object {
        private val log = logger<ISPPanelPopulator>()
    }

    fun populateToolWindowPanel(panel: JComponent) {
        log.info("Initializing tool window content")
        CoroutineScope(SupervisorJob() + Dispatchers.Default).async {
            return@async Pair(
                SourceScanner.findShadcnImplementation(project),
                FileManager(project).getVirtualFilesByName("package.json").size
            )
        }.asCompletableFuture().thenApplyAsync { (source, packageJsonCount) ->
            log.info("Shadcn implementation detected: $source, package.json count: $packageJsonCount")
            panel.removeAll()
            if (source == null) {
                panel.add(JBLabel("No shadcn/ui implementation detected.", SwingConstants.CENTER))
            } else if (packageJsonCount > 1) {
                panel.add(JBLabel("Multiple projects detected, not supported yet.", SwingConstants.CENTER))
            } else {
                panel.add(ISPWindowContents(source).panel())
            }
            panel.revalidate()
        }
        log.info("Tool window content initialized")
    }
}
