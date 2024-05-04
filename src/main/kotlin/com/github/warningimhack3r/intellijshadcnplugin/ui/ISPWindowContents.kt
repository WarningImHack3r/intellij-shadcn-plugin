package com.github.warningimhack3r.intellijshadcnplugin.ui

import com.github.warningimhack3r.intellijshadcnplugin.backend.sources.Source
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.*
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

class ISPWindowContents(private val source: Source<*>) {
    companion object {
        private val log = logger<ISPWindowContents>()
    }

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

    fun panel() = JPanel(GridLayout(0, 1)).apply {
        border = JBUI.Borders.empty(10)

        val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var installedComponents = emptyList<String>()
        coroutineScope.launch {
            installedComponents = runReadAction { source.getInstalledComponents() }
        }.invokeOnCompletion { throwable ->
            if (throwable != null && throwable !is CancellationException) {
                return@invokeOnCompletion
            }
            // Add a component panel
            add(createPanel("Add a component") {
                coroutineScope.async {
                    runReadAction { source.fetchAllComponents() }.map { component ->
                        Item(
                            component.name,
                            "${
                                // Convert the component name to a human-readable title
                                component.name.replace("-", " ").replaceFirstChar {
                                    if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
                                }
                            } component for ${source.framework}",
                            listOf(
                                LabeledAction("Add", CompletionAction.DISABLE_ROW) {
                                    runWriteAction { source.addComponent(component.name) }
                                }
                            ),
                            installedComponents.contains(component.name)
                        )
                    }.also {
                        log.info("Fetched and rendering ${it.size} remote components: ${it.joinToString(", ") { component -> component.title }}")
                    }
                }.asCompletableFuture()
            }.apply {
                border = BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, JBUI.CurrentTheme.ToolWindow.borderColor()),
                    JBUI.Borders.emptyBottom(10)
                )
            })

            // Manage components panel
            add(createPanel("Manage components", coroutineScope.async {
                val shouldDisplay =
                    runReadAction {
                        installedComponents.any { component -> !source.isComponentUpToDate(component) }
                    }
                if (shouldDisplay) {
                    JButton("Update all").apply {
                        addActionListener {
                            isEnabled = false
                            installedComponents.forEach { component ->
                                runWriteAction { source.addComponent(component) }
                            }
                            // TODO: Update the list's row actions
                            val par = parent
                            par.remove(this)
                            par.revalidate()
                        }
                    }
                    null // Remove once the update mechanism is implemented
                } else null
            }.asCompletableFuture()) {
                coroutineScope.async {
                    installedComponents.map { component ->
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
                    }.also {
                        log.info("Fetched and rendering ${it.size} installed components: ${it.joinToString(", ") { component -> component.title }}")
                    }
                }.asCompletableFuture()
            }.apply {
                border = JBUI.Borders.emptyTop(10)
            })
        }

        log.info("Successfully created initial panel")
    }

    private fun createPanel(
        title: String,
        rightHandComponent: CompletableFuture<JComponent?> = CompletableFuture.completedFuture(null),
        listContents: () -> CompletableFuture<List<Item>>
    ) =
        JPanel().apply panel@{
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
            val searchBar = JBTextField().apply {
                maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
                addKeyListener(object : KeyAdapter() {
                    override fun keyReleased(e: KeyEvent) {
                        if (scrollPane == null) return
                        this@panel.remove(scrollPane)
                        scrollPane = componentsList(items.filter {
                            it.title.lowercase().contains(text.lowercase())
                                    || it.subtitle?.lowercase()?.contains(text.lowercase()) == true
                        })
                        this@panel.add(scrollPane)
                        this@panel.revalidate()
                    }
                })
            }
            add(JPanel(BorderLayout()).apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                add(searchBar)
                rightHandComponent.thenApplyAsync { component ->
                    component?.let {
                        it.maximumSize = Dimension(it.maximumSize.width, searchBar.maximumSize.height)
                        add(it)
                        revalidate()
                    }
                }
            })

            // Loading spinner
            val spinner = JPanel(BorderLayout()).apply {
                add(JLabel("Loading...").apply {
                    horizontalAlignment = SwingConstants.CENTER
                }, BorderLayout.CENTER)
            }
            add(spinner)

            // Components list
            listContents()
                .thenApplyAsync {
                    items = it
                    log.info("Rendering ${it.size} items for panel $title")
                    titledBorder.title = "$title (${it.size})"
                    scrollPane = componentsList(items)
                    remove(spinner)
                    add(scrollPane)
                    revalidate()
                }
        }

    private fun componentsList(rows: List<Item>) = JBScrollPane().apply {
        setViewportView(JPanel(GridLayout(0, 1)).apply {
            rows.forEach { row ->
                add(createRow(row))
            }
            add(JPanel())
        })
    }

    private fun createRow(rowData: Item) = JPanel(BorderLayout()).apply row@{
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
            add(JPanel().apply actions@{
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
