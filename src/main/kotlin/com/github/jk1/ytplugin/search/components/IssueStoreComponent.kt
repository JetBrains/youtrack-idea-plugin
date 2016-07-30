package com.github.jk1.ytplugin.search.components

import com.github.jk1.ytplugin.common.YouTrackServer
import com.github.jk1.ytplugin.search.model.Issue
import com.github.jk1.ytplugin.search.rest.IssuesRestClient
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ActionCallback
import java.util.concurrent.ConcurrentHashMap

class IssueStoreComponent(val project: Project) : AbstractProjectComponent(project) {

    private val stores = ConcurrentHashMap<YouTrackServer, Store>()

    operator fun get(repo: YouTrackServer): Store {
        return stores.getOrPut(repo, { Store(repo) })
    }

    inner class Store(private val repo: YouTrackServer) {
        private val client = IssuesRestClient(project, repo)
        private var issues: List<Issue> = listOf()
        private var currentCallback: ActionCallback = ActionCallback.Done()
        private val listeners = mutableSetOf({ /** todo: fileStore().save() */ })

        fun update(): ActionCallback {
            if (isUpdating()) {
                return currentCallback
            }
            currentCallback = refresh()
            return currentCallback
        }

        private fun refresh(): ActionCallback {
            val future = ActionCallback()
            object : Task.Backgroundable(project, "Updating issues from server", true, PerformInBackgroundOption.ALWAYS_BACKGROUND) {
                override fun run(indicator: ProgressIndicator) {
                    try {
                        issues = client.getIssues(repo.defaultSearch)
                    } catch (e: Exception) {
                        // todo: notification and logging
                        e.printStackTrace()
                    }
                }

                override fun onCancel() {
                    future.setDone()
                }

                override fun onSuccess() {
                    future.setDone()
                    listeners.forEach { it.invoke() }
                }
            }.queue()
            return future
        }


        fun isUpdating() = !currentCallback.isDone

        fun getAllIssues(): Collection<Issue> = issues

        fun getIssue(index: Int)= issues[index]

        fun addListener(listener: () -> Unit){
            listeners.add(listener)
        }
    }
}