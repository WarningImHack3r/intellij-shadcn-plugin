package com.github.warningimhack3r.intellijshadcnplugin.ui

import com.github.warningimhack3r.intellijshadcnplugin.backend.helpers.FileManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import icons.ISPIcons
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import javax.swing.SwingConstants

class ISPToolWindow: ToolWindowFactory {
    override fun init(toolWindow: ToolWindow) {
        ApplicationManager.getApplication().invokeLater {
            toolWindow.setIcon(ISPIcons.logo)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        with(toolWindow.contentManager) {
            addContent(factory.createContent(SimpleToolWindowPanel(true).apply {
                GlobalScope.async {
                    return@async runReadAction {
                        FileManager(project).getVirtualFilesByName("package.json").size
                    }
                }.asCompletableFuture().thenApplyAsync { count ->
                    if (count > 1) {
                        add(JBLabel("Multiple projects detected, not supported yet.", SwingConstants.CENTER))
                    } else {
                        add(ISPWindowContents(project).panel())
                    }
                }
            }, null, false))
        }
    }
}
