package com.github.jk1.ytplugin.issues

import com.github.jk1.ytplugin.issues.PersistentIssueStore.Memento
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
 * Application-scoped persistent issue data cache. Issue data is persisted in a home folder instead of
 * a project directory. This comes in handy for projects with /.idea under VCS control.
 */
@Service
@State(name = "YouTrack Issues", storages = [(Storage(value = "issues.xml"))])
class PersistentIssueStore : PersistentStateComponent<Memento> {

    private val stores = ConcurrentHashMap<String, IssueStore>()

    override fun getState() = Memento(stores)

    override fun loadState(state: Memento) {
        stores.putAll(state.persistentIssues.map { (repoKey, issuesJson) ->
            try {
                val issues = JsonParser.parseString(issuesJson).asJsonArray
                    .mapNotNull { IssueJsonParser.parseIssue(it, repoKey) }

                logger.debug("Issue store file cache loaded for $repoKey with a total of ${issues.size}")
                Pair(repoKey, IssueStore(issues))
            } catch (e: Exception) {
                logger.warn("Failed to load issue store file cache for $repoKey", e)
                Pair(repoKey, IssueStore())
            }
        })
    }

    operator fun get(repo: YouTrackServer): IssueStore {
        return stores.getOrPut(repo.id) {
            logger.debug("No persistent issue store found for ${repo.url}, will create an empty one")
            IssueStore()
        }
    }

    fun remove(repo: YouTrackServer) {
        stores.remove(repo.id)
    }

    class Memento constructor() {

        // should stay mutable and public for serialization to work
        var persistentIssues: Map<String, String> = mutableMapOf()


        // primary constructor is reserved for serializer
        constructor(stores: Map<String, IssueStore>) : this() {
            persistentIssues = stores.mapValues { "[${it.value.joinToString(", ") { it.json }}]" }
        }
    }
}
