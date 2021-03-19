package com.github.jk1.ytplugin.workflowsDebugConfiguration.ui

import com.github.jk1.ytplugin.workflowsDebugConfiguration.WorkflowRulesHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.GuiUtils
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import java.awt.FlowLayout
import java.util.*
import javax.swing.*

open class WorkflowNameEntryDialog(val project: Project) : DialogWrapper(project, false) {

    private val workflowNameField = GuiUtils.createUndoableTextField()
    private val okButton = JButton("Load")
    private val cancelButton = JButton("Cancel")


    init {
        title = "Load Workflow To Local Files"
        rootPane.defaultButton = okButton
    }

    override fun show() {
        init()
        super.show()
    }

    fun createMainPanel(): JComponent {
        okButton.addActionListener { okAction() }

        return FormBuilder.createFormBuilder()
                .addLabeledComponent("Workflow Name", workflowNameField)
                .panel
    }

    private fun createButtonsPanel(): JPanel {
        val buttonsPanel = JPanel(FlowLayout(2))

        okButton.addActionListener { okAction() }
        cancelButton.addActionListener { super.doCancelAction() }

        buttonsPanel.add(cancelButton)
        buttonsPanel.add(okButton)

        return buttonsPanel
    }

    override fun createActions(): Array<out Action> = arrayOf()

    override fun createCenterPanel(): JComponent {
        val contextPane = JPanel(VerticalFlowLayout())
        contextPane.apply {
            preferredSize = Dimension(400, 90)
            minimumSize = preferredSize
            add(createMainPanel())
            add(createButtonsPanel())
        }
        return contextPane
    }

    private fun okAction() {
        WorkflowRulesHandler().loadWorkflowRules(workflowNameField.text, project)
        this@WorkflowNameEntryDialog.close(0)
    }

}
