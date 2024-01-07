package com.github.warningimhack3r.intellijshadcnplugin.ui

import com.github.warningimhack3r.intellijshadcnplugin.backend.SourceScanner
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.asCompletableFuture
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.swing.*
import javax.swing.border.CompoundBorder
import javax.swing.border.MatteBorder
import javax.swing.border.TitledBorder

class ISPWindowContents(private val project: Project) {
    data class Item(
        val title: String,
        val subtitle: String?,
        val actions: List<LabeledAction>,
        val disabled: Boolean = false
    )

    data class LabeledAction(
        val label: String,
        val actionOnEnd: CompletionAction,
        val action: () -> Unit
    )

    enum class CompletionAction {
        REMOVE_TRIGGER,
        REMOVE_ROW,
        DISABLE_ROW
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun panel() = JPanel(GridLayout(0, 1)).apply {
        border = JBUI.Borders.empty(10)

        // Add a component panel
        add(createPanel("Add a component") {
            GlobalScope.async {
                val source = runReadAction { SourceScanner.findShadcnImplementation(project) }
                if (source == null) return@async emptyList()
                val installedComponents = runReadAction { source.getInstalledComponents() }
                runReadAction { source.fetchAllComponents() }.map { component ->
                    Item(
                        component.name,
                        component.description ?: "${component.name.replace("-", " ")
                            .replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                            }} component for ${source.framework}",
                        listOf(
                            LabeledAction("Add", CompletionAction.DISABLE_ROW) {
                                runWriteAction { source.addComponent(component.name) }
                            }
                        ),
                        installedComponents.contains(component.name)
                    )
                }
            }.asCompletableFuture()
        }.apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBUI.CurrentTheme.ToolWindow.borderColor()),
                JBUI.Borders.emptyBottom(10)
            )
        })

        // Manage components panel
        add(createPanel("Manage components") {
            GlobalScope.async {
                val source = runReadAction { SourceScanner.findShadcnImplementation(project) }
                if (source == null) return@async emptyList()
                runReadAction { source.getInstalledComponents() }.map { component ->
                    Item(
                        component,
                        null,
                        listOfNotNull(
                            LabeledAction("Update", CompletionAction.REMOVE_TRIGGER) {
                                runWriteAction { source.addComponent(component) }
                            }.takeIf {
                                runReadAction { !source.isComponentUpToDate(component) }
                            },
                            LabeledAction("Remove", CompletionAction.REMOVE_ROW) {
                                runWriteAction { source.removeComponent(component) }
                            }
                        )
                    )
                }
            }.asCompletableFuture()
        }.apply {
            border = JBUI.Borders.emptyTop(10)
        })
    }

    private fun createPanel(title: String, listContents: () -> CompletableFuture<List<Item>>) = JPanel().apply panel@ {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        // Title
        val titledBorder = TitledBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, JBUI.CurrentTheme.ToolWindow.borderColor()),
            title
        )
        add(JPanel().apply {
            minimumSize = Dimension(Int.MAX_VALUE, preferredSize.height + 20)
            maximumSize = Dimension(Int.MAX_VALUE, minimumSize.height)
            border = titledBorder
        })
        var items: List<Item> = emptyList()
        var scrollPane: JBScrollPane? = null
        // Search bar
        add(JBTextField().apply {
            maximumSize = Dimension(Int.MAX_VALUE, this.preferredSize.height)
            addKeyListener(object : KeyAdapter() {
                override fun keyReleased(e: KeyEvent) {
                    if (scrollPane != null) {
                        this@panel.remove(scrollPane)
                        scrollPane = componentsList(items.filter {
                            it.title.lowercase().contains(text.lowercase())
                                    || it.subtitle?.lowercase()?.contains(text.lowercase()) == true
                        })
                        this@panel.add(scrollPane)
                        this@panel.revalidate()
                    }
                }
            })
        })
        // Components list
        listContents()
            .thenApplyAsync {
                items = it
                titledBorder.title = "$title (${it.size})"
                scrollPane = componentsList(items)
                add(scrollPane)
                revalidate()
            }
    }

    private fun componentsList(rows: List<Item>) = JBScrollPane().apply {
        setViewportView(JPanel(GridLayout(0, 1)).apply {
            rows.forEach { row ->
                add(createRow(row))
            }
        })
    }

    private fun createRow(rowData: Item) = JPanel(BorderLayout()).apply row@ {
        border = CompoundBorder(
            MatteBorder(0, 0, 1, 0, JBUI.CurrentTheme.ToolWindow.borderColor()),
            JBUI.Borders.empty(10)
        )

        // Title and subtitle vertically stacked
        add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(textWrappingLabel(rowData.title))
            rowData.subtitle?.let { subtitle ->
                add(textWrappingLabel(subtitle, JBUI.CurrentTheme.Label.disabledForeground()))
            }
        }, BorderLayout.PAGE_START)

        // Actions horizontally stacked
        add(JPanel(BorderLayout()).apply {
            add(JPanel().apply actions@ {
                layout = BoxLayout(this, BoxLayout.X_AXIS)

                val progressBar = JProgressBar().apply {
                    border = JBUI.Borders.emptyRight(10)
                    preferredSize = Dimension(100, preferredSize.height)
                    isIndeterminate = true
                    isVisible = false
                }
                add(progressBar)
                rowData.actions.forEach { action ->
                    add(JButton(action.label).apply {
                        isEnabled = !rowData.disabled
                        addActionListener {
                            isEnabled = false
                            progressBar.isVisible = !isEnabled
                            action.action()
                            isEnabled = true
                            progressBar.isVisible = !isEnabled
                            when (action.actionOnEnd) {
                                CompletionAction.REMOVE_TRIGGER -> parent.remove(this)
                                CompletionAction.REMOVE_ROW -> {
                                    this@row.parent.remove(this@row)
                                    // TODO: Update the other list & both counters
                                }
                                CompletionAction.DISABLE_ROW -> {
                                    this@actions.components.forEach { it.isEnabled = false }
                                    // TODO: Update the other list & both counters
                                }
                            }
                        }
                    })
                }
            }, BorderLayout.LINE_END)
        }, BorderLayout.PAGE_END)
    }

    private fun textWrappingLabel(text: String, foregroundColor: Color = JBUI.CurrentTheme.Label.foreground()) =
        JTextArea(text).apply {
            wrapStyleWord = true
            lineWrap = true
            isEditable = false
            font = JBUI.Fonts.label()
            foreground = foregroundColor
            background = UIManager.getColor("Label.background")
            border = UIManager.getBorder("Label.border")
        }
}
