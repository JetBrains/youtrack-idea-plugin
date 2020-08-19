package com.github.jk1.ytplugin.issues

import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.IssueJsonParser
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonParser
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.util.concurrent.ConcurrentHashMap

/**
 * Application-scoped persistent issue work items data cache. Issue work items data is persisted in a home folder instead of
 * a project directory.
 */
@Service
@State(name = "YouTrack IssuesWorkItems", storages = [(Storage(value = "issuesWorkItems.xml"))])
class PersistentIssueWorkItemsStore : PersistentStateComponent<PersistentIssueWorkItemsStore.Memento> {

    private var loadedMemento: Memento = Memento()
    private val stores = ConcurrentHashMap<String, IssueWorkItemStore>()

    override fun getState(): Memento? = Memento(stores)

    override fun loadState(state: Memento) {
        loadedMemento = state
    }

    operator fun get(repo: YouTrackServer): IssueWorkItemStore {
        return stores.getOrPut(repo.id, {
            logger.debug("IssueWorkItems store opened for YouTrack server ${repo.url}")
            loadedMemento.getStore(repo)
        })
    }

    fun remove(repo: YouTrackServer) {
        stores.remove(repo.id)
    }

    class Memento constructor() {

        // should stay mutable and public for serialization to work
       var persistentIssueWorkItems: Map<String, String> = mutableMapOf()

        // primary constructor is reserved for serializer
        constructor(stores: Map<String, IssueWorkItemStore>) : this() {
            persistentIssueWorkItems = stores.mapValues { "[${it.value.joinToString(", ") { it.author }}]" }
        }

        fun getStore(repo: YouTrackServer): IssueWorkItemStore {
            try {
                val issuesWorkItemsJson = persistentIssueWorkItems[repo.id] ?: return IssueWorkItemStore()
                val issuesWorkItems = JsonParser.parseString(issuesWorkItemsJson).asJsonArray
                        .mapNotNull { IssueJsonParser.parseWorkItem(it) }
                logger.debug("IssueWorkItems store file cache loaded for ${repo.url} with a total of ${issuesWorkItems.size}")
                return IssueWorkItemStore(issuesWorkItems)
            } catch (e: Exception) {
                logger.warn("Failed to load issueWorkItems store file cache for ${repo.url}", e)
                return IssueWorkItemStore()
            }
        }
    }
}