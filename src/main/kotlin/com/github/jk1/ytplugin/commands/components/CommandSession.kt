package com.github.jk1.ytplugin.commands.components

import com.github.jk1.ytplugin.common.components.ComponentAware
import com.github.jk1.ytplugin.search.model.IssueTask
import com.github.jk1.ytplugin.search.rest.IssuesRestClient
import com.intellij.openapi.project.Project

/**
 * Resolves and encodes persistent issue id once per command dialog. Encoded database id is required
 * by the most effective command suggestion REST method in YouTrack. YouTrack 5.2 does not expose
 * persistent ids via rest, making this optimization impossible.
 */
class CommandSession(override val project: Project) : ComponentAware {

    val task = taskManagerComponent.getActiveTask()
    val compressedEntityId: String?

    init {
        val entityId: String?
        if (task is IssueTask) {
            entityId = task.issue.entityId
        } else {
            // try local store first, fall back to rest api if not found
            val repo = taskManagerComponent.getActiveYouTrackRepository()
            val issue = issueStoreComponent[repo].firstOrNull { it.id.equals(task.id) }
                    ?: IssuesRestClient(repo).getIssue(task.id)
            entityId = issue?.entityId
        }
        compressedEntityId = when (entityId) {
            null -> null
            else -> encodePersistentId(entityId.split("-")[1].toInt())
        }
    }

    fun hasEntityId() = compressedEntityId != null

    /**
     * Type id is removed, the remainder is split into 6-bit chunks, each encoded as a single symbol.
     */
    private fun encodePersistentId(number: Int, buffer: String = ""): String = when {
        number > 0 -> encodePersistentId(number.shr(6), buffer) + toChar(number.and(63))
        else -> buffer
    }

    /**
     * ASCII table conversion:
     * [0, 9]   -> ASCII 48-57, numbers
     * [10, 35] -> ASCII 65-89, uppercase letters
     * [36, 63] -> ASCII 97-122, lowercase letters
     */
    private fun toChar(number: Int) = when {
        number < 10 -> 48 + number
        number < 36 -> 55 + number
        else -> 61 + number
    }.toChar().toString()

}