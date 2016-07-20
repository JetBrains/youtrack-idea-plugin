package com.github.jk1.ytplugin.search.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.intellij.tasks.Comment
import com.intellij.tasks.Task
import com.intellij.tasks.TaskType
import com.intellij.tasks.impl.LocalTaskImpl
import java.util.*
import javax.swing.Icon

class Issue(item: JsonElement) {

    val id: String
    val entityId: String
    val summary: String
    val description: String
    val createDate: Date
    val updateDate: Date
    val resolved: Boolean
    val customFields: Map<String, String> = mutableMapOf()

    init {
        val root = item.asJsonObject
        id = root.get("id").asString
        entityId = root.get("entityId").asString
        summary = getPredefinedFieldValue("summary", root) ?: ""
        description = getPredefinedFieldValue("description", root) ?: ""
        createDate = Date(getPredefinedFieldValue("created", root)?.toLong() ?: 0)
        updateDate = Date(getPredefinedFieldValue("updated", root)?.toLong() ?: 0)
        resolved = root.getAsJsonArray("field").any { "resolved".equals(it.asJsonObject.get("name").asString) }
    }

    private fun getPredefinedFieldValue(name: String, root: JsonObject): String? {
        return root.getAsJsonArray("field").firstOrNull {
            name.equals(it.asJsonObject.get("name").asString)
        }?.asJsonObject?.get("value")?.asString
    }

    fun asTask(): Task = object : Task() {

        override fun getId(): String = this@Issue.id

        override fun getSummary(): String = this@Issue.summary

        override fun getDescription(): String = this@Issue.description

        override fun getCreated() = this@Issue.createDate

        override fun getUpdated() = this@Issue.updateDate

        override fun isClosed() = this@Issue.resolved

        override fun getComments(): Array<out Comment> = arrayOf()

        override fun getIcon(): Icon = LocalTaskImpl.getIconFromType(type, isIssue)

        override fun getType(): TaskType = TaskType.OTHER

        override fun isIssue() = true

        // todo: proper url
        override fun getIssueUrl() = "http://lol.de"
    }
}