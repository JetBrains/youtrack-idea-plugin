package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import java.util.concurrent.ConcurrentHashMap

/**
 * Application-scoped persistent Time tracker cache. Time tracker data is persisted in a home folder instead of
 * a project directory. This comes in handy for projects with /.idea under VCS control.
 */
@Service
@State(name = "YouTrack time tracker", storages = [(Storage(value = "timeTracker.xml"))])
class PersistentTimeTrackerStore : PersistentStateComponent<PersistentTimeTrackerStore.Memento>{

    private var loadedMemento: Memento = Memento()
    private val stores = ConcurrentHashMap<String, TimeTrackerStore>()

    override fun getState() = Memento(stores)

    override fun loadState(state: Memento) {
        loadedMemento = state
    }

    operator fun get(repo: YouTrackServer): TimeTrackerStore {
        return stores.getOrPut(repo.id, {
            logger.debug("Time tracker store is loaded for ${repo.url}")
            loadedMemento.getStore(repo)
        })
    }

    fun remove(repo: YouTrackServer) {
        stores.remove(repo.id)
    }

    class Memento constructor() {

        // should stay mutable and public for serialization to work
        var persistentTimeTracker: Map<String, String> = mutableMapOf()

        // primary constructor is reserved for serializer
        constructor(stores: Map<String, TimeTrackerStore>) : this() {
            persistentTimeTracker = stores.mapValues {
                it.value.getTrackerJson() }
        }

        fun getStore(repo: YouTrackServer): TimeTrackerStore {
            try {
                val timeTrackerJson = persistentTimeTracker[repo.id] ?: return TimeTrackerStore()
                logger.debug("Time tracker file cache loaded for ${repo.url} ")
                return TimeTrackerStore(timeTrackerJson)
            } catch (e: Exception) {
                logger.warn("Failed to load time tracker store file cache for ${repo.url}", e)
                return TimeTrackerStore()
            }
        }
    }
}
