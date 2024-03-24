package com.github.warningimhack3r.intellijshadcnplugin.ui

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import icons.ISPIcons

class ISPToolWindow : ToolWindowFactory {
    companion object {
        private val log = logger<ISPToolWindow>()
    }

    override fun init(toolWindow: ToolWindow) {
        log.info("Initializing shadcn/ui plugin v${PluginManagerCore.getPlugin(
            PluginId.getId("com.github.warningimhack3r.intellijshadcnplugin")
        )?.version ?: "???"}")
        ApplicationManager.getApplication().invokeLater {
            log.debug("Initializing tool window with icon")
            toolWindow.setIcon(ISPIcons.logo)
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        with(toolWindow.contentManager) {
            addContent(factory.createContent(SimpleToolWindowPanel(true).apply {
                while (!project.isInitialized) {
                    // wait for project to be initialized
                }
                log.debug("Project initialized, starting to scan for shadcn implementation")
                ISPPanelPopulator(project).populateToolWindowPanel(this)
            }, null, false))
        }
    }
}
