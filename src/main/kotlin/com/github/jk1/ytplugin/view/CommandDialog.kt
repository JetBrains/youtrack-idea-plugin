package com.github.jk1.ytplugin.view

import com.github.jk1.ytplugin.components.CommandComponent
import com.github.jk1.ytplugin.components.ComponentAware
import com.github.jk1.ytplugin.lang.CommandLanguage
import com.github.jk1.ytplugin.model.CommandAssistResponse
import com.github.jk1.ytplugin.model.CommandPreview
import com.github.jk1.ytplugin.model.YouTrackCommand
import com.github.jk1.ytplugin.model.YouTrackCommandExecution
import com.intellij.openapi.editor.event.DocumentListener
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


class CommandDialog(override val project: Project) : DialogWrapper(project, false), ComponentAware {

    private val commandField = createCommandField()
    private val commentArea = JTextArea(6, 60)
    private val visibilityGroupDropdown = JComboBox<String>()
    private val previewLabel = JLabel()

    private val applyAction = ExecuteCommandAction("Apply")
    private val silentApplyAction = ExecuteCommandAction("Silent Apply", true)

    init {
        title = "Apply Command"
        // todo: lazy loading for permitted groups
        adminComponent.getUserGroups().forEach { visibilityGroupDropdown.addItem(it) }
        init()
    }

    override fun createActions(): Array<out Action> = arrayOf(applyAction, silentApplyAction, cancelAction)

    override fun createJButtonForAction(action: Action): JButton {
        val button = super.createJButtonForAction(action)
        button.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "apply");
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
        file?.putUserData(CommandComponent.USER_DATA_KEY, CommandSuggestListener())
        peer.window.addWindowFocusListener(object : WindowAdapter() {
            override fun windowGainedFocus(e: WindowEvent) {
                commandField.requestFocusInWindow()
            }
        })
        // todo: find a better way to attach onEnter handler to LanguageTextField
        commandField.addDocumentListener(object: DocumentListener {
            override fun documentChanged(p0: com.intellij.openapi.editor.event.DocumentEvent?) {
                val component = commandField.editor!!.contentComponent
                component.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "apply");
                component.actionMap.put("apply", applyAction)
            }

            override fun beforeDocumentChange(p0: com.intellij.openapi.editor.event.DocumentEvent?) {
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
        with(taskManagerComponent.getActiveTask()) {
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
    inner class ExecuteCommandAction(name: String, val silent: Boolean = false) : AbstractAction(name) {
        override fun actionPerformed(e: ActionEvent) {
            val command = YouTrackCommand(commandField.text, commandField.caretModel.offset)
            val group = visibilityGroupDropdown.selectedItem.toString()
            val execution = YouTrackCommandExecution(command, silent, commentArea.text, group)
            commandComponent.executeAsync(execution)
            this@CommandDialog.close(0)
        }
    }

    /**
     * Formats parsed command preview as html and displays it in command window
     */
    inner class CommandSuggestListener() : CommandComponent by commandComponent {
        override fun suggest(command: YouTrackCommand): CommandAssistResponse {
            val response = commandComponent.suggest(command)
            SwingUtilities.invokeLater {
                val previewList = response.previews.mapIndexed { i, preview ->
                    "${i + 1}. ${preview.html()}"
                }.joinToString("<br/>")
                previewLabel.text = "<html>$previewList</html>"
            }
            return response
        }

        fun CommandPreview.html() = if (error) "<span style='color:red'>$description</span>" else description
    }
}