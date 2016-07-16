package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.common.components.ComponentAware
import com.github.jk1.ytplugin.search.model.Issue
import com.intellij.openapi.project.Project
import javax.swing.AbstractListModel

class IssuesModel(override val project: Project) : AbstractListModel<Issue>(), ComponentAware {

    override fun getElementAt(index: Int): Issue? {
        return issueStoreComponent.getIssue(issueStoreComponent.getSortedIssues()[index])
    }

    override fun getSize(): Int {
        return issueStoreComponent.getSortedIssues().count()
    }
}