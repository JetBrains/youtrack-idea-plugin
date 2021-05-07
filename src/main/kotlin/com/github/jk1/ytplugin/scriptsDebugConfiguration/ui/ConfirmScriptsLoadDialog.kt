package com.github.jk1.ytplugin.scriptsDebugConfiguration.ui

import com.github.jk1.ytplugin.scriptsDebugConfiguration.ScriptsRulesHandler
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.components.Label
import java.awt.Dimension
import java.awt.FlowLayout
import java.util.*
import javax.swing.*

open class ConfirmScriptsLoadDialog(val project: Project) : DialogWrapper(project, false) {

    private val okButton = JButton("Load")
    private val cancelButton = JButton("Cancel")


    init {
        title = "Load Scripts From YouTrack"
        rootPane.defaultButton = okButton
    }

    override fun show() {
        init()
        super.show()
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
            preferredSize = Dimension(300, 70)
            minimumSize = preferredSize
            add(Label("Load scripts from YouTrack?"))
            add(createButtonsPanel())
        }
        return contextPane
    }


    private fun okAction() {
        ScriptsRulesHandler(project).loadWorkflowRules()
        this@ConfirmScriptsLoadDialog.close(0)
    }

}
