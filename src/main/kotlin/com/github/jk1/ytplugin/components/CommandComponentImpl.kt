package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.model.YouTrackCommand
import com.github.jk1.ytplugin.rest.CommandRestClient
import com.github.jk1.ytplugin.sendNotification
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.tasks.youtrack.YouTrackIntellisense
import com.intellij.util.ConcurrencyUtil
import com.intellij.util.containers.hash.LinkedHashMap
import javax.swing.SwingUtilities


class CommandComponentImpl(override val project: Project) :
        AbstractProjectComponent(project), CommandComponent, ComponentAware {

    companion object {
        val executor = ConcurrencyUtil.newSingleThreadExecutor("YouTrack command executor")
        val LOG = Logger.getInstance(YouTrackIntellisense::class.java)
    }

    val restClient = CommandRestClient(project)

    override fun executeAsync(command: YouTrackCommand) {
        executor.submit {
            try {
                command.issues.add(taskManagerComponent.getActiveTask())
                val result = restClient.executeCommand(command)
                SwingUtilities.invokeLater {
                    result.errors.forEach { sendNotification("Command execution error", it, NotificationType.ERROR) }
                    result.messages.forEach { sendNotification("YouTrack server message", it, NotificationType.INFORMATION) }
                }
            } catch(e: Throwable) {
                //todo: redirect to event log
                e.printStackTrace()
            }
        }
    }

    override fun getIntellisense(): YouTrackIntellisense {
        return object : YouTrackIntellisense(taskManagerComponent.getYouTrackRepository()) {
            override fun fetchCompletion(query: String, caret: Int): List<CompletionItem> {
                return fetch(query, caret, false).completionItems
            }

            override fun fetchHighlighting(query: String, caret: Int): List<HighlightRange> {
                return fetch(query, caret, true).highlightRanges
            }

            private fun fetch(query: String, caret: Int, ignoreCaret: Boolean): YouTrackIntellisense.Response {
                LOG.debug("Query: \'$query\' caret at: $caret")
                val lookup = Query(query, caret)
                var response: YouTrackIntellisense.Response? = null
                if (ignoreCaret) {
                    val url = IntellisenseCache.keys.iterator()
                    while (url.hasNext()) {
                        val startTime = url.next()
                        if (startTime.command.equals(query)) {
                            response = IntellisenseCache[startTime] as YouTrackIntellisense.Response
                            break
                        }
                    }
                } else {
                    response = IntellisenseCache[lookup]
                }

                LOG.debug("Cache " + (if (response != null) "hit" else "miss"))
                if (response == null) {
                    val command = YouTrackCommand(query, caret)
                    command.issues.add(taskManagerComponent.getActiveTask())
                    response = restClient.getIntellisense(command)
                    IntellisenseCache.put(lookup, response)
                }

                return response
            }
        }
    }

    private class Query(val command: String, val caret: Int)

    private object IntellisenseCache : LinkedHashMap<Query, YouTrackIntellisense.Response>(40, true) {
        override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<Query, YouTrackIntellisense.Response>,
                key: Query, value: YouTrackIntellisense.Response):
                Boolean = this.size > 30

    }
}