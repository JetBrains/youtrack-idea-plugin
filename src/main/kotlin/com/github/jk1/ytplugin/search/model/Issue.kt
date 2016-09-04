package com.github.jk1.ytplugin.search.model

import com.github.jk1.ytplugin.common.logger
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.intellij.tasks.Task
import com.intellij.tasks.TaskType
import com.intellij.tasks.impl.LocalTaskImpl
import java.util.*
import javax.swing.Icon

class Issue(item: JsonElement, val repoUrl: String) {

    private val PREDEFINED_FIELDS = arrayOf("projectShortName", "numberInProject", "summary",
            "description", "created", "updated", "updaterName", "updaterFullName", "resolved",
            "reporterName", "reporterFullName", "commentsCount", "votes", "attachments", "links",
            "sprint", "voterName", "permittedGroup")

    val id: String
    val entityId: String?      // youtrack 5.2 doesn't expose entity id via rest
    val summary: String
    val description: String
    val createDate: Date
    val updateDate: Date
    val resolved: Boolean
    val customFields: List<CustomField>
    val comments: List<IssueComment>

    init {
        val root = item.asJsonObject
        id = root.get("id").asString
        entityId = root.get("entityId")?.asString
        summary = getFieldValue("summary", root) ?: ""
        description = getFieldValue("description", root) ?: ""
        createDate = Date(getFieldValue("created", root)?.toLong() ?: 0)
        updateDate = Date(getFieldValue("updated", root)?.toLong() ?: 0)
        resolved = getFieldValue("resolved", root) != null
        customFields = root.getAsJsonArray("field")
                .filter { it.isCustomField() }
                .map { parseCustomFieldSafe(it) }
                .filter { it != null }
                .requireNoNulls()
        comments = root.getAsJsonArray("comment").map { IssueComment(it) }
        // todo: parse tags, links
    }

    override fun toString() = "$id $summary" // Quick search in issue list relies on that

    private fun parseCustomFieldSafe(item: JsonElement): CustomField? {
        try {
            return CustomField(item)
        } catch(e: Exception) {
            logger.warn("YouTrack issue parsing error: custom field cannot be parsed. Offending element: $item")
            logger.debug(e)
            return null
        }
    }

    private fun getFieldValue(name: String, root: JsonObject): String? {
        return root.getAsJsonArray("field").firstOrNull {
            name.equals(it.asJsonObject.get("name").asString)
        }?.asJsonObject?.get("value")?.asString
    }

    private fun JsonElement.isCustomField(): Boolean {
        val name = asJsonObject.get("name")
        return name != null && !PREDEFINED_FIELDS.contains(name.asString)
    }

    fun asTask() = IssueTask(this, repoUrl)
}


