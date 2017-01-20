package com.github.jk1.ytplugin.issues

import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.IssueJsonParser
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonParser
import com.intellij.openapi.components.ApplicationComponent
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.util.concurrent.ConcurrentHashMap

@State(name = "YouTrack Issues", storages = arrayOf(Storage(value = "issues.xml")))
class PersistentIssueStoreComponent() : ApplicationComponent, PersistentStateComponent<PersistentIssueStoreComponent.Memento> {

    private var loadedMemento: Memento = Memento()
    private val stores = ConcurrentHashMap<String, IssueStore>()

    override fun initComponent() { }

    override fun disposeComponent() {}

    override fun getComponentName(): String = javaClass.canonicalName

    override fun getState() = Memento(stores.values)

    override fun loadState(state: Memento?) {
        loadedMemento = state!!
    }

    operator fun get(repo: YouTrackServer): IssueStore {
        return stores.getOrPut(repo.id, {
            logger.debug("Issue store opened for YouTrack server ${repo.url}")
            loadedMemento.getStore(repo) ?: IssueStore(repo)
        })
    }

    fun remove(repo: YouTrackServer) {
       stores.remove(repo.id)
    }

    class Memento constructor() {

        // implementation should stay mutable as deserializer calls #clear() on it
        var persistentIssues: Map<String, String> = mutableMapOf()

        // primary constructor is reserved for serializer
        constructor(stores: Iterable<IssueStore>) : this() {
            persistentIssues = stores.associate { Pair(it.repo.id, "[${it.map { it.json }.joinToString(", ")}]") }
        }

        fun getStore(repo: YouTrackServer): IssueStore? {
            // todo: handle read errors
            val issuesJson = persistentIssues[repo.id] ?: return null
            val issues = JsonParser().parse(issuesJson).asJsonArray
                    .map { IssueJsonParser.parseIssue(it, repo.url) }
                    .filterNotNull()
            return IssueStore(repo, issues)
        }
    }
}