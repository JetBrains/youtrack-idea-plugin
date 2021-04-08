package com.github.jk1.ytplugin.tasks

import com.github.jk1.ytplugin.issues.model.Issue
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.tasks.Task
import com.intellij.tasks.youtrack.YouTrackIntellisense
import com.intellij.tasks.youtrack.YouTrackRepository

/**
 * Wraps task management plugin repository to provide handy accessor operations.
 * This is a snapshot implementation and may not reflect the latest changes made to delegate object.
 */
class YouTrackServer(private val delegate: YouTrackRepository, val project: Project) {

    val id: String get() = "${project.name} $username@$url"
    val url: String get() = delegate.url
    val username: String get() = delegate.username
    val password: String get() = delegate.password
    val useProxy: Boolean get() = delegate.isUseProxy

    var defaultSearch: String
        get() = synchronized(delegate) {
            delegate.defaultSearch
        }
        set(value) {
            synchronized(delegate) {
                delegate.defaultSearch = value
            }
        }

    fun getSearchCompletionProvider() = YouTrackIntellisense(delegate)

    fun createTask(issue: Issue): Task = IssueTask(issue, delegate)

    fun getTasks(query: String?, offset: Int, limit: Int): Array<Task>
            = delegate.getIssues(query, offset, limit, true, DumbProgressIndicator())

    fun getRepo(): YouTrackRepository{
        return delegate
    }
}