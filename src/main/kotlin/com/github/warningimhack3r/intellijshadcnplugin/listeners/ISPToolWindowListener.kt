package com.github.warningimhack3r.intellijshadcnplugin.listeners

import com.github.warningimhack3r.intellijshadcnplugin.ui.ISPWindowContents
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener

class ISPToolWindowListener(private val project: Project) : ToolWindowManagerListener {
    private val toolWindowId = "shadcn/ui"
    private var isToolWindowOpen: Boolean? = null

    override fun stateChanged(toolWindowManager: ToolWindowManager) {
        val previousState = isToolWindowOpen
        isToolWindowOpen = toolWindowManager.getToolWindow(toolWindowId)?.isVisible ?: false
        if (previousState == false && isToolWindowOpen == true) {
            toolWindowManager.getToolWindow(toolWindowId)?.contentManager?.getContent(0)?.let {
                with(it.component) {
                    if (components.isEmpty()) return@let
                    remove(components[0])
                    add(ISPWindowContents(project).panel())
                    revalidate()
                }
            }
        }
    }
}
