package com.github.jk1.ytplugin.issues.model

import com.github.jk1.ytplugin.YouTrackIssueField
import com.github.jk1.ytplugin.asColor
import com.google.gson.JsonElement
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*

class CustomField(item: JsonElement) : YouTrackIssueField {

    val name: String
    val value: List<String>
    private val valueId: List<String>
    var foregroundColor: Color? = null
    var backgroundColor: Color? = null


    init {
        name = if (item.asJsonObject.get("name") != null && !item.asJsonObject.get(("name")).isJsonNull){
            item.asJsonObject.get("name").asString
        } else {
            // YouTack 2018.X fallback
            item.asJsonObject.get("projectCustomField").asJsonObject.get("field").asJsonObject.get("name").asString
        }
        valueId = mutableListOf()
        valueId.add(item.asJsonObject.get("id").asString)

        val valueItem = item.asJsonObject.get("value")

        if (valueItem == null || valueItem.isJsonNull || (valueItem.isJsonArray && valueItem.asJsonArray.size() == 0)) {
            value = listOf(item.asJsonObject.get("projectCustomField").asJsonObject.get("emptyFieldText").asString)
        } else {
            if (valueItem.isJsonArray) {
                value = mutableListOf()

                for (currValue in valueItem.asJsonArray) {
                    value.add(currValue.asJsonObject.get("name").asString)
                    if (currValue.asJsonObject.get("color") != null) {
                        val color = currValue.asJsonObject.get("color")
                        foregroundColor = color.asJsonObject.get("foreground").asColor()
                        backgroundColor = color.asJsonObject.get("background").asColor()
                    }
                }
            } else if (valueItem.isJsonPrimitive) {
                value = listOf(valueItem.asString)
            } else {
                // must be JSON Object
                if (valueItem.asJsonObject.get("color") != null) {
                    val color = valueItem.asJsonObject.get("color")
                    foregroundColor = color.asJsonObject.get("foreground").asColor()
                    backgroundColor = color.asJsonObject.get("background").asColor()
                }

                value = mutableListOf()
                if (valueItem.asJsonObject.get("presentation") == null)
                    value.add(valueItem.asJsonObject.get("name").asString)
                else
                    value.add(item.asJsonObject.get("name").asString + ": " + valueItem.asJsonObject.get("presentation").asString)
            }
        }
    }

    override fun getFieldName() = name

    override fun getFieldValues() = value

    fun formatValues() = " ${value.joinToString { formatValue(it) }} "

    private fun formatValue(value: String): String {
        return if (value.matches(Regex("^[1-9][0-9]{12}"))) { // looks like a timestamp
            SimpleDateFormat().format(Date(value.toLong()))
        } else {
            value
        }
    }
}