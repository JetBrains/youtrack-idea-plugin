package com.github.jk1.ytplugin.commands.components

import com.github.jk1.ytplugin.common.components.ComponentAware
import com.github.jk1.ytplugin.search.model.IssueTask
import com.intellij.openapi.project.Project


class CommandSession(override val project: Project) : ComponentAware {

    val task = taskManagerComponent.getActiveTask()
    val entityId: String?
    val compressedEntityId: String?

    init {
        if (task is IssueTask) {
            entityId = task.issue.entityId
        } else {
            val repo = taskManagerComponent.getActiveYouTrackRepository()
            entityId = issueStoreComponent[repo].firstOrNull { it.id.equals(task.id) }?.entityId
        }
        compressedEntityId = when (entityId) {
            null -> null
            else -> encodePersistentId(entityId.split("-")[1].toInt())
        }
    }

    fun hasEntityId() = entityId != null

    private fun encodePersistentId(number: Int, buffer: String = ""): String = when {
        number > 0 -> encodePersistentId(number.shr(6), buffer) + toChar(number.and(63))
        else -> buffer
    }

    private fun toChar(number: Int) = when {
        number < 10 -> 48 + number
        number < 36 -> 55 + number
        else -> 61 + number
    }.toChar().toString()

}