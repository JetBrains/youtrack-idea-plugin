package com.github.jk1.ytplugin.issues

import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.IssueJsonParser
import com.google.gson.JsonParser
import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

@State(name = "YouTrack Issues", storages = arrayOf(Storage(file = "issues.xml")))
class IssueStoreComponent(val project: Project) : AbstractProjectComponent(project),
        PersistentStateComponent<IssueStoreComponent.Memento> {

    private var loadedMemento: Memento = Memento()
    private val stores = ConcurrentHashMap<String, IssueStore>()
    private val timedRefreshTask = JobScheduler.getScheduler().scheduleWithFixedDelay({
        SwingUtilities.invokeLater { stores.forEach { it.value.update() } }
    }, 5, 5, TimeUnit.MINUTES)     //  todo: customizable update interval

    operator fun get(repo: YouTrackServer): IssueStore {
        return stores.getOrPut(repo.id, {
            logger.debug("Issue store opened for project ${project.name} and YouTrack server ${repo.url}")
            loadedMemento.getStore(repo) ?: IssueStore(repo)
        })
    }

    override fun projectClosed() {
        timedRefreshTask.cancel(false)
    }

    override fun getState() = Memento(stores.values)

    override fun loadState(state: Memento?) {
        loadedMemento = state!!
    }

    class Memento constructor() {

        // implementation should stay mutable as deserializer calls #clear() on it
        var persistentIssues: Map<String, String>  = mutableMapOf()

        // primary constructor is reserved for serializer
        constructor(stores: Iterable<IssueStore>): this(){
            persistentIssues = stores.associate { Pair(it.repo.id, "[${it.map { it.json }.joinToString(", ")}]") }
        }

        fun getStore(repo: YouTrackServer) : IssueStore?{
            val issuesJson = persistentIssues[repo.id] ?: return null
            val issues = JsonParser().parse(issuesJson).asJsonArray
                    .map { IssueJsonParser.parseIssue(it, repo.url) }
                    .filterNotNull()
            return IssueStore(repo, issues)
        }
    }
}