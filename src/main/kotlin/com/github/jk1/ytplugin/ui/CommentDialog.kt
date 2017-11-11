package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.commands.CommandSession
import com.intellij.openapi.project.Project

class CommentDialog(project: Project, session: CommandSession): CommandDialog(project, session) {

    override val focusRoot = commentArea

    override fun init() {
        commandField.text = "comment"
        super.init()
    }
}