package com.github.jk1.ytplugin.view

import com.github.jk1.ytplugin.components.CommandComponent
import com.github.jk1.ytplugin.components.ComponentAware
import com.github.jk1.ytplugin.lang.CommandLanguage
import com.github.jk1.ytplugin.model.CommandPreview
import com.github.jk1.ytplugin.model.YouTrackCommand
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.LanguageTextField
import java.awt.BorderLayout
import java.awt.KeyboardFocusManager
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*

public class CommandDialog(override val project: Project) : DialogWrapper(project), ComponentAware {

    private val commandField = LanguageTextField(CommandLanguage, project, "")
    private val commentArea = JTextArea(6, 40)
    private val visibilityGroupDropdown = JComboBox<String>()
    private val previewLabel = JLabel()

    init {
        title = "Apply Command"
        previewLabel.text = "<html><font color='red'>1. Unknown command</font></html>"
        adminComponent.getUserGroups().forEach { visibilityGroupDropdown.addItem(it) }
        // Setup document for completion and highlighting
        val file = PsiDocumentManager.getInstance(project).getPsiFile(commandField.document)
        file?.putUserData(CommandComponent.USER_DATA_KEY, commandComponent)
        commandField.addDocumentListener(UpdatePreviewDocumentListener())
        peer.window.addWindowFocusListener(object : WindowAdapter() {
            override fun windowGainedFocus(e: WindowEvent) {
                commandField.requestFocusInWindow()
            }
        })
        init()
    }

    override fun createActions(): Array<out Action> {
        return arrayOf(getApplyAction(), getSilentApplyAction(), this.cancelAction)
    }

    override fun createCenterPanel(): JComponent {
        val contextPane = JPanel(BorderLayout())
        contextPane.add(createLeftPane(), BorderLayout.WEST)
        contextPane.add(createPreviewPanel(), BorderLayout.EAST)
        return contextPane
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
            panel.add(JLabel("Command for: $id $adaptedSummary"), BorderLayout.NORTH)
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
        panel.add(JLabel("Comment: "), BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
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
        panel.add(previewContainer, BorderLayout.CENTER)
        return panel
    }

    private fun getApplyAction(): Action {
        return object : AbstractAction("Apply") {
            override fun actionPerformed(e: ActionEvent) {
                val command = YouTrackCommand(commandField.text, commandField.caretModel.offset, false)
                commandComponent.executeAsync(command)
                this@CommandDialog.close(0)
            }
        }
    }

    private fun getSilentApplyAction(): Action {
        return object : AbstractAction("Silent Apply") {
            override fun actionPerformed(e: ActionEvent) {
                val command = YouTrackCommand(commandField.text, commandField.caretModel.offset, true)
                commandComponent.executeAsync(command)
                this@CommandDialog.close(0)
            }
        }
    }

    /**
     * Formats parsed command preview as html and displays it in command window
     */
    inner class UpdatePreviewDocumentListener() : DocumentListener {

        override fun documentChanged(event: DocumentEvent) {
            val command = YouTrackCommand(commandField.text, commandField.caretModel.offset, false)
            val response = commandComponent.suggest(command)
            val previewList = response.previews.map { "<li>${it.html()}</li>" }.joinToString()
            previewLabel.text = "<html><ol>$previewList</ol></html>"
        }

        fun CommandPreview.html() = if (error) "<span style='color:red'>$description</span>" else description

        override fun beforeDocumentChange(event: DocumentEvent) {
        }
    }
}