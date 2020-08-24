package com.github.jk1.ytplugin.issues.model

import com.google.gson.JsonElement
import java.util.*


class IssueWorkItem(item: JsonElement) : Comparable<IssueWorkItem> {

    val type: String =  if (item.asJsonObject.get("type").isJsonNull)
                            "None"
                        else
                            item.asJsonObject.get("type").asJsonObject.get("name").asString

    val issueId: String = item.asJsonObject.get("issue").asJsonObject.get("idReadable").asString
    val date: Date = Date(item.asJsonObject.get("date").asLong)
    val created: Date = Date(item.asJsonObject.get("created").asLong)
    val value: String = item.asJsonObject.get("duration").asJsonObject.get("presentation").asString
            .replace("h", " hours")
            .replace("d", " days")
            .replace("w", " weeks")
            .replace("m", " minutes")

    val author: String = item.asJsonObject.get("author").asJsonObject.get("name").asString
    val id: String = item.asJsonObject.get("id").asString

    val comment: String? = if (!item.asJsonObject.get("text").isJsonNull)
        item.asJsonObject.get("text").asString
    else
        null

    override operator fun compareTo(other: IssueWorkItem): Int {
        return date.compareTo(other.date)
    }
}
