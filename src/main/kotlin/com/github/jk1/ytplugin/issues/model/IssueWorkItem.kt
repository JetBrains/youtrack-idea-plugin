package com.github.jk1.ytplugin.issues.model

import com.google.gson.JsonElement
import java.util.*


class IssueWorkItem(item: JsonElement) : Comparable<IssueWorkItem> {

    val json: String = item.toString()
    val issueId: String = item.asJsonObject.get("issue").asJsonObject.get("idReadable").asString
    val date: Date = Date(item.asJsonObject.get("date").asLong)
    val value: String = item.asJsonObject.get("duration").asJsonObject.get("presentation").asString

    val type: String =  if (item.asJsonObject.get("type").isJsonNull)
        "None"
    else
        item.asJsonObject.get("type").asJsonObject.get("name").asString

    val author: String = item.asJsonObject.get("author").asJsonObject.get("name").asString
    val id: String = item.asJsonObject.get("id").asString
    val created: Date = Date(item.asJsonObject.get("created").asLong)

    val comment: String? = if (!item.asJsonObject.get("text").isJsonNull)
        item.asJsonObject.get("text").asString
    else
        null

    override operator fun compareTo(other: IssueWorkItem): Int {
        return date.compareTo(other.date)
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