package com.github.jk1.ytplugin.view

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import javax.swing.JComponent

public class CommandDialog(project: Project) : DialogWrapper(project){

    override fun createCenterPanel(): JComponent? {
        return null
    }
}