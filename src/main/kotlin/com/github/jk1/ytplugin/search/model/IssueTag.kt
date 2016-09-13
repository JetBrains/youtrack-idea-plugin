package com.github.jk1.ytplugin.search.model

import com.google.gson.JsonElement
import java.awt.Color

class IssueTag(item: JsonElement) {

    val text: String
    val foregroundColor: Color
    val backgroundColor: Color
    val borderColor: Color

    init {
        val tagColor = cssClassToColor[item.asJsonObject.get("cssClass").asString]!!
        foregroundColor = tagColor.foreground
        backgroundColor = tagColor.background
        borderColor = tagColor.foreground
        text = item.asJsonObject.get("value").asString
    }

    companion object {
        val cssClassToColor = mapOf(
                "c0" to TagColor("#000000", "#ffffff"), "c1" to TagColor("#ffffff", "#8d5100"),
                "c2" to TagColor("#ffffff", "#ce6700"), "c3" to TagColor("#ffffff", "#409600"),
                "c4" to TagColor("#ffffff", "#0070e4"), "c5" to TagColor("#ffffff", "#900052"),
                "c6" to TagColor("#ffffff", "#0050a1"), "c7" to TagColor("#ffffff", "#2f9890"),
                "c8" to TagColor("#ffffff", "#8e1600"), "c9" to TagColor("#ffffff", "#dc0083"),
                "c10" to TagColor("#ffffff", "#7dbd36"), "c11" to TagColor("#ffffff", "#ff7123"),
                "c12" to TagColor("#ffffff", "#ff7bc3"), "c13" to TagColor("#444444", "#fed74a"),
                "c14" to TagColor("#444444", "#b7e281"), "c15" to TagColor("#45818e", "#d8f7f3"),
                "c16" to TagColor("#888888", "#e6e6e6"), "c17" to TagColor("#4da400", "#e6f6cf"),
                "c18" to TagColor("#b45f06", "#ffee9c"), "c19" to TagColor("#444444", "#ffc8ea"),
                "c20" to TagColor("#ffffff", "#e30000"), "c21" to TagColor("#3d85c6", "#e0f1fb"),
                "c22" to TagColor("#dc5766", "#fce5f1"), "c23" to TagColor("#b45f06", "#f7e9c1"),
                "c24" to TagColor("#444444", "#92e1d5"), "c25" to TagColor("#444444", "#a6e0fc"),
                "c26" to TagColor("#444444", "#e0c378"), "c27" to TagColor("#444444", "#bababa"),
                "c28" to TagColor("#ffffff", "#25beb2"), "c29" to TagColor("#ffffff", "#42a3df"),
                "c30" to TagColor("#ffffff", "#878787"), "c31" to TagColor("#ffffff", "#4d4d4d"),
                "c32" to TagColor("#ffffff", "#246512"), "c33" to TagColor("#ffffff", "#00665e"),
                "c34" to TagColor("#ffffff", "#553000"), "c35" to TagColor("#ffffff", "#1a1a1a")
        )
    }

    class TagColor(foreground: String, background: String) {
        val foreground: Color = Color.decode(foreground)
        val background: Color = Color.decode(background)
    }
}