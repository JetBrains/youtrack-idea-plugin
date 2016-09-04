package com.github.jk1.ytplugin.search.model

import com.intellij.tasks.Task
import com.intellij.tasks.TaskType
import com.intellij.tasks.impl.LocalTaskImpl
import javax.swing.Icon

/**
 * Adapter class to use YouTrack issues as Task Management plugin tasks
 */
class IssueTask(val issue: Issue, val repoUrl: String): Task() {

    override fun getId(): String = issue.id

    override fun getSummary(): String = issue.summary

    override fun getDescription(): String = issue.description

    override fun getCreated() = issue.createDate

    override fun getUpdated() = issue.updateDate

    override fun isClosed() = issue.resolved

    override fun getComments() = issue.comments.map { it.asTaskManagerComment() }.toTypedArray()

    override fun getIcon(): Icon = LocalTaskImpl.getIconFromType(type, isIssue)

    override fun getType(): TaskType = TaskType.OTHER

    override fun isIssue() = true

    override fun getIssueUrl() = "$repoUrl/issue/$id"
}