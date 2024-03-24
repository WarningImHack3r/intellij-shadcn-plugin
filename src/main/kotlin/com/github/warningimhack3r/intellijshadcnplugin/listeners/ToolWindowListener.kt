package com.github.warningimhack3r.intellijshadcnplugin.listeners

import com.github.warningimhack3r.intellijshadcnplugin.ui.ISPPanelPopulator
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener

class ToolWindowListener(private val project: Project) : ToolWindowManagerListener {
    companion object {
        private val log = logger<ToolWindowListener>()
        private const val TOOL_WINDOW_ID = "shadcn/ui"
    }
    private var isToolWindowOpen: Boolean? = null

    override fun stateChanged(toolWindowManager: ToolWindowManager) {
        val previousState = isToolWindowOpen
        isToolWindowOpen = toolWindowManager.getToolWindow(TOOL_WINDOW_ID)?.isVisible ?: false
        if (previousState == false && isToolWindowOpen == true) {
            log.info("Tool window was closed and is now open, updating contents")
            toolWindowManager.getToolWindow(TOOL_WINDOW_ID)?.contentManager?.getContent(0)?.let {
                with(it.component) {
                    if (components.isEmpty()) return@let
                    log.info("Removing old contents and adding new ones")
                    ISPPanelPopulator(project).populateToolWindowPanel(this)
                }
            }
        }
    }
}
