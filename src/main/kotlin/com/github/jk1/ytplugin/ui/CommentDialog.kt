package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.issues.model.Issue
import com.intellij.openapi.project.Project

/*
 * Commenting is implemented though prefilled command window
 */
class CommentDialog(project: Project, issue: Issue) : CommandDialog(project, issue) {

    override val focusRoot = commentArea

    override fun init() {
        commandField.text = "comment"
        super.init()
    }
}