package com.github.jk1.ytplugin.view

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

public class CommandDialog(project: Project) : DialogWrapper(project){

    init {
        title = "Apply Command"
        init()
    }

    override fun createCenterPanel(): JComponent? {
        val contextPane = JPanel(BorderLayout());
        contextPane.add(JLabel("Command window contents"), BorderLayout.NORTH);
        contextPane.add(JLabel("Command window contents"), BorderLayout.WEST);
        contextPane.add(JLabel("Command window contents"), BorderLayout.EAST);
        return contextPane;
    }
}