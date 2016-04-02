package com.github.jk1.ytplugin.commands.components

import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.common.components.ComponentAware
import com.intellij.openapi.project.Project
import com.intellij.util.containers.hash.LinkedHashMap
import java.util.concurrent.TimeUnit

/**
 * Command assist response cache to make command completion more responsive and avoid UI lags. This is
 * especially helpful for large YouTrack installations, where command backend is known to be slow to
 * respond from time to time.
 */
class CommandSuggestResponseCache(override val project: Project): ComponentAware {

    private val cache = SuggestResponseCache()

    operator fun get(command: YouTrackCommand?): CommandAssistResponse? {
        synchronized(this) {
            return cache[CommandCacheKey(command, getUrl())]
        }
    }

    operator fun set(command: YouTrackCommand?, value: CommandAssistResponse) {
        synchronized(this) {
            cache.put(CommandCacheKey(command, getUrl()), value)
        }
    }

    private fun getUrl(): String = taskManagerComponent.getActiveYouTrackRepository().url

    data class CommandCacheKey(val command: YouTrackCommand?, val serverUrl: String)

    inner class SuggestResponseCache : LinkedHashMap<CommandCacheKey, CommandAssistResponse>(10, true) {

        val CACHE_ENTRY_TTL = TimeUnit.MILLISECONDS.convert(30, TimeUnit.MINUTES)

        override fun removeEldestEntry(
                eldest: MutableMap.MutableEntry<CommandCacheKey, CommandAssistResponse>,
                key: CommandCacheKey, value: CommandAssistResponse):
                Boolean = this.size > 30

        override fun get(key: CommandCacheKey?): CommandAssistResponse? {
            super.get(key)?.let {
                if (Math.abs(System.currentTimeMillis() - it.timestamp) > CACHE_ENTRY_TTL) {
                    remove(key)
                }
            }
            return super.get(key)
        }
    }
}