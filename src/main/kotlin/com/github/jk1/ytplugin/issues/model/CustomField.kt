package com.github.jk1.ytplugin.issues.model

import com.github.jk1.ytplugin.YouTrackIssueField
import com.google.gson.JsonElement
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*

class CustomField(item: JsonElement): YouTrackIssueField {

    val name: String
    val value: List<String>
    private val valueId: List<String>
    var foregroundColor: Color? = null
    var backgroundColor: Color? = null


    init {
        name = item.asJsonObject.get("name").asString
        valueId = mutableListOf()
        valueId.add(item.asJsonObject.get("id").asString)
        val valueItem = item.asJsonObject.get("value")
        if (valueItem == null || valueItem.isJsonNull ||(valueItem.isJsonArray && valueItem.asJsonArray.size() == 0)) {
            value = emptyList()
        }
        else {
            value = mutableListOf()
            value.add(valueItem.asJsonObject.get("name").asString)
        }
    }

    override fun getFieldName() = name

    override fun getFieldValues() = value

    private fun JsonElement.asColor() = when {
    // #F0A -> #FF00AA
        asString.length == 4 -> Color.decode(asString.drop(1).map { "$it$it" }.joinToString("", "#"))
        else -> Color.decode(asString)
    }

    fun formatValues() = " ${value.joinToString { formatValue(it) }} "

    private fun formatValue(value: String): String {
        if (value.matches(Regex("^[1-9][0-9]{12}"))) { // looks like a timestamp
            return SimpleDateFormat().format(Date(value.toLong()))
        } else {
            return value
        }
    }
}