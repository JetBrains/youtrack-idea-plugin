package com.github.jk1.ytplugin.search.model

import com.google.gson.JsonElement
import java.awt.Color

class CustomField(item: JsonElement) {

    val name: String
    val value: List<String>
    val valueId: List<String>
    var foregroundColor: Color? = null
    var backgroundColor: Color? = null

    init {
        name = item.asJsonObject.get("name").asString
        val valueIdItem = item.asJsonObject.get("valueId")
        val valueItem = item.asJsonObject.get("value")
        if (valueIdItem == null || valueIdItem.isJsonNull){
            // User type field uses a different presentation
            value = valueItem.asJsonArray.map { it.asJsonObject["fullName"].asString }
            valueId = valueItem.asJsonArray.map { it.asJsonObject["value"].asString }
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

    private fun JsonElement.asColor() = Color.decode(asString)
}