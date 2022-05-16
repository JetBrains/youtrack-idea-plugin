package com.github.jk1.ytplugin.issues.model

import com.github.jk1.ytplugin.rest.IssueJsonParser
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import java.util.*


class IssueWorkItem(item: JsonElement) : Comparable<IssueWorkItem> {

    val json: String = item.toString()
    val root =  item.asJsonObject

    val issueId: String = root.get("issue").asJsonObject.get("idReadable").asString
    val date: Date = Date(root.get("date").asLong)
    val value: String = root.get("duration").asJsonObject.get("presentation").asString

    val type: String =  if (root.get("type").isJsonNull)
        "None"
    else
        root.get("type").asJsonObject.get("name").asString

    val author: String = root.get("author").asJsonObject.get("name").asString
    val id: String = root.get("id").asString
    val created: Date = Date(root.get("created").asLong)

    val comment: String? = if (!root.get("text").isJsonNull)
        root.get("text").asString
    else
        null

    var attributes: MutableList<WorkItemAttribute> = mutableListOf()

    override operator fun compareTo(other: IssueWorkItem): Int {
        return date.compareTo(other.date)
    }

    init {
        if (root.getAsJsonArray("attributes") != null && !root.getAsJsonArray("attributes").isJsonNull) {
            val attributesJson: JsonArray = root.getAsJsonArray("attributes")
            attributesJson.mapNotNull { attributes.add(IssueJsonParser.parseWorkItemAttribute(it)!!) }
            attributes = attributes.filter { it.value != null && it.value.isNotEmpty() }.toMutableList()
        }
    }

    /**
     * Could be used for instances in english only. Muted till localization is completed
     */
    private fun formatTimePresentation(inputTime: String) : String{
        val weeks: Long
        val days: Long
        val hours: Long
        val minutes: Long

        var time = inputTime.replace("\\s".toRegex(), "")
        var result = ""

        var sepPos: Int = time.lastIndexOf("w")
        if (sepPos != -1) {
            weeks = time.substring(0, sepPos).toLong()
            result += weeks
            result += if (weeks > 1){
                " weeks "
            } else {
                " week "
            }
            time = time.substring(sepPos + 1, time.length)
        }
        sepPos = time.lastIndexOf("d")
        if (sepPos != -1) {
            days = time.substring(0, sepPos).toLong()
            result += days
            result += if (days  > 1){
                " days "
            } else {
                " day "
            }
            time = time.substring(sepPos + 1, time.length)
        }
        sepPos = time.lastIndexOf("h")
        if (sepPos != -1) {
            hours = time.substring(0, sepPos).toLong()
            result += hours
            result += if (hours > 1){
                " hours "
            } else {
                " hour "
            }
            time = time.substring(sepPos + 1, time.length)
        }
        sepPos = time.lastIndexOf("m")
        if (sepPos != -1) {
            minutes = time.substring(0, sepPos).toLong()
            result += minutes
            result += if (minutes > 1){
                " minutes "
            } else {
                " minute "
            }
        }
        return result
    }
}