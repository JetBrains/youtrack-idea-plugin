package com.github.jk1.ytplugin.setup.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.project.DumbAware
import javax.swing.Icon
import javax.swing.JComponent

abstract class SetupAction : AnAction(), DumbAware {

    abstract val text: String
    abstract val description: String
    abstract val icon: Icon
    abstract val shortcut: String

    fun register(parent: JComponent) {
        templatePresentation.text = text
        templatePresentation.description = description
        templatePresentation.icon = icon
        registerCustomShortcutSet(CustomShortcutSet.fromString(shortcut), parent)
    }
}