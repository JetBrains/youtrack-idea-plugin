package com.github.jk1.ytplugin.search.components

import com.github.jk1.ytplugin.search.model.Issue
import com.github.jk1.ytplugin.search.rest.IssuesRestClient
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ActionCallback
import com.intellij.tasks.impl.BaseRepository
import com.intellij.util.containers.SortedList
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class IssueStoreComponent(val project: Project) : AbstractProjectComponent(project) {

    private val stores = ConcurrentHashMap<BaseRepository, Store>()

    operator fun get(repo: BaseRepository): Store {
        return stores.getOrPut(repo, { Store(repo) })
    }

    inner class Store(repo: BaseRepository) {
        private val client = IssuesRestClient(project, repo)
        private val issues = HashMap<String, Issue>()
        private val sortedIssues = SortedList(Comparator<String> { o1, o2 ->
            o1.compareTo(o2)
        })
        private var currentCallback: ActionCallback = ActionCallback.Done()

        var searchQuery = ""

        fun getAllIssues(): Collection<Issue> {
            return issues.values
        }

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
                        setIssues(client.getIssues(searchQuery))
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
                    //fileStore().save()
                }
            }.queue()
            return future
        }

        private fun setIssues(updatedIssues: List<Issue>) {
            issues.putAll(updatedIssues.associateBy { it.id })
            sortedIssues.clear()
            sortedIssues.addAll(this.issues.keys)
        }

        fun isUpdating(): Boolean {
            return !currentCallback.isDone
        }


        fun getIssue(id: String): Issue? {
            return issues[id]
        }

        fun getSortedIssues(): SortedList<String> {
            return sortedIssues
        }
    }


}