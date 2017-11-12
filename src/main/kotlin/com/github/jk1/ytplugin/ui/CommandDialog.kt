package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.commands.CommandComponent
import com.github.jk1.ytplugin.commands.CommandSession
import com.github.jk1.ytplugin.commands.lang.CommandLanguage
import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.CommandPreview
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.editor.AdminComponent
import com.intellij.openapi.editor.event.DocumentAdapter
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.LanguageTextField
import java.awt.BorderLayout
import java.awt.KeyboardFocusManager
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*


open class CommandDialog(override val project: Project, val session: CommandSession) : DialogWrapper(project, false), ComponentAware {

    protected val commandField = createCommandField()
    protected val commentArea = JTextArea(6, 60)
    protected val visibilityGroupDropdown = JComboBox<String>()
    protected val previewLabel = JLabel()
    protected open val focusRoot: JComponent = commandField

    private val applyAction = ExecuteCommandAction("Apply")
    private val silentApplyAction = ExecuteCommandAction("Silent Apply", true)

    override fun init() {
        title = "Apply Command"
        // put placeholder value while group list is loaded in background
        visibilityGroupDropdown.addItem(AdminComponent.ALL_USERS)
        adminComponent.getActiveTaskVisibilityGroups(session.issue) { groups ->
            SwingUtilities.invokeLater {
                visibilityGroupDropdown.removeAllItems()
                groups.forEach { visibilityGroupDropdown.addItem(it) }
            }
        }
        super.init()
        peer.window.addWindowFocusListener(object : WindowAdapter() {
            override fun windowGainedFocus(e: WindowEvent) {
                focusRoot.requestFocusInWindow()
            }
        })
    }

    override fun show() {
        init()
        super.show()
    }

    override fun createActions(): Array<out Action> = arrayOf(applyAction, silentApplyAction, cancelAction)

    override fun createJButtonForAction(action: Action): JButton {
        val button = super.createJButtonForAction(action)
        button.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "apply")
        button.actionMap.put("apply", action)
        return button
    }

    override fun createCenterPanel(): JComponent {
        val contextPane = JPanel(BorderLayout())
        contextPane.add(createLeftPane(), BorderLayout.WEST)
        contextPane.add(createPreviewPanel(), BorderLayout.EAST)
        return contextPane
    }

    private fun createCommandField(): LanguageTextField {
        val commandField = LanguageTextField(CommandLanguage, project, "")
        // Setup document for completion and highlighting
        val file = PsiDocumentManager.getInstance(project).getPsiFile(commandField.document)
        file?.putUserData(CommandComponent.COMPONENT_KEY, CommandSuggestListener())
        file?.putUserData(CommandComponent.SESSION_KEY, session)
        // todo: find a better way to attach onEnter handler to LanguageTextField
        commandField.addDocumentListener(object : DocumentAdapter() {
            override fun documentChanged(event: DocumentEvent) {
                val component = commandField.editor?.contentComponent
                if (component != null) {
                    component.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "apply")
                    component.actionMap.put("apply", applyAction)
                }
            }
        })
        return commandField
    }

    private fun createLeftPane(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.add(createCommandStringPanel(), BorderLayout.NORTH)
        panel.add(createCommentPanel(), BorderLayout.CENTER)
        panel.add(createVisibilityGroupPanel(), BorderLayout.SOUTH)
        panel.border = BorderFactory.createEmptyBorder(0, 0, 0, 20)
        return panel
    }

    private fun createCommandStringPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        with(session.issue) {
            val adaptedSummary = if (summary.length > 40) "${summary.substring(0, 40)}..." else summary
            val label = JLabel("Command for: $id $adaptedSummary")
            label.border = BorderFactory.createEmptyBorder(0, 0, 2, 0)
            panel.add(label, BorderLayout.NORTH)
        }
        panel.add(commandField, BorderLayout.CENTER)
        return panel
    }

    private fun createCommentPanel(): JPanel {
        // reset Tab key behavior to transfer focus from the text area instead of adding tab symbols
        commentArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null)
        commentArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null)
        val panel = JPanel(BorderLayout())
        val scrollPane = JScrollPane(commentArea)
        val label = JLabel("Comment: ")
        panel.add(label, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        label.border = BorderFactory.createEmptyBorder(0, 0, 2, 0)
        panel.border = BorderFactory.createEmptyBorder(10, 0, 10, 0)
        return panel
    }

    private fun createVisibilityGroupPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        panel.add(JLabel("Visible for Group: "), BorderLayout.WEST)
        panel.add(visibilityGroupDropdown, BorderLayout.CENTER)
        return panel
    }

    private fun createPreviewPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        val previewContainer = JPanel(BorderLayout())
        previewContainer.add(previewLabel, BorderLayout.NORTH)
        previewContainer.border = BorderFactory.createEmptyBorder(10, 0, 0, 0)
        panel.add(JLabel("Command Preview:                                               "), BorderLayout.NORTH)
        panel.add(previewContainer, BorderLayout.WEST)
        return panel
    }

    /**
     * Submits command for async execution and closes command dialog immediately
     */
    inner class ExecuteCommandAction(name: String, private val silent: Boolean = false) : AbstractAction(name) {
        override fun actionPerformed(e: ActionEvent) {
            val group = visibilityGroupDropdown.selectedItem.toString()
            val execution = YouTrackCommandExecution(session, commandField.text, silent, commentArea.text, group)
            commandComponent.executeAsync(execution)
            this@CommandDialog.close(0)
        }
    }

    /**
     * Formats parsed command preview as html and displays it in command window
     */
    inner class CommandSuggestListener : CommandComponent by commandComponent {
        override fun suggest(command: YouTrackCommand): CommandAssistResponse {
            val response = commandComponent.suggest(command)
            SwingUtilities.invokeLater {
                val previewList = response.previews.mapIndexed { i, preview ->
                    "${i + 1}. ${preview.html()}"
                }.joinToString("<br/>")
                previewLabel.text = "<html><body style='width:180px'>$previewList"
            }
            return response
        }

        private fun CommandPreview.html() = if (error) "<span style='color:red'>$description</span>" else description
    }
}