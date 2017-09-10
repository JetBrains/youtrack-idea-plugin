package com.github.jk1.ytplugin.issues.model

import com.google.gson.JsonElement
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*

class CustomField(item: JsonElement) {

    val name: String
    val value: List<String>
    private val valueId: List<String>
    var foregroundColor: Color? = null
    var backgroundColor: Color? = null

    init {
        name = item.asJsonObject.get("name").asString
        val valueIdItem = item.asJsonObject.get("valueId")
        val valueItem = item.asJsonObject.get("value")
        if (valueIdItem == null || valueIdItem.isJsonNull) {
            value = valueItem.asJsonArray.map {
                if (it.isJsonObject) {
                    // User type field uses a different presentation
                    it.asJsonObject["fullName"].asString
                } else {
                    // 5.2 does not return value id
                    it.asString
                }
            }
            valueId = valueItem.asJsonArray.map {
                if (it.isJsonObject) {
                    it.asJsonObject["value"].asString
                } else {
                    // 5.2 does not return value id
                    it.asString
                }
            }
        } else {
            value = valueItem.asJsonArray.map { it.asString }
            valueId = valueIdItem.asJsonArray.map { it.asString }
        }
        val color = item.asJsonObject.get("color")
        if (color != null && !color.isJsonNull) {
            foregroundColor = color.asJsonObject.get("fg").asColor()
            backgroundColor = color.asJsonObject.get("bg").asColor()
        }
    }

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