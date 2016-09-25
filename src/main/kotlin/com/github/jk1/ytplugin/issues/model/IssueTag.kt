package com.github.jk1.ytplugin.issues.model

import com.google.gson.JsonElement
import java.awt.Color

class IssueTag(item: JsonElement) {

    val text: String
    val foregroundColor: Color
    val backgroundColor: Color
    val borderColor: Color

    init {
        val tagColor = IssueTag.Companion.cssClassToColor[item.asJsonObject.get("cssClass").asString]!!
        foregroundColor = tagColor.foreground
        backgroundColor = tagColor.background
        borderColor = tagColor.foreground
        text = item.asJsonObject.get("value").asString
    }

    companion object {
        val cssClassToColor = mapOf(
                "c0" to IssueTag.TagColor("#000000", "#ffffff"), "c1" to IssueTag.TagColor("#ffffff", "#8d5100"),
                "c2" to IssueTag.TagColor("#ffffff", "#ce6700"), "c3" to IssueTag.TagColor("#ffffff", "#409600"),
                "c4" to IssueTag.TagColor("#ffffff", "#0070e4"), "c5" to IssueTag.TagColor("#ffffff", "#900052"),
                "c6" to IssueTag.TagColor("#ffffff", "#0050a1"), "c7" to IssueTag.TagColor("#ffffff", "#2f9890"),
                "c8" to IssueTag.TagColor("#ffffff", "#8e1600"), "c9" to IssueTag.TagColor("#ffffff", "#dc0083"),
                "c10" to IssueTag.TagColor("#ffffff", "#7dbd36"), "c11" to IssueTag.TagColor("#ffffff", "#ff7123"),
                "c12" to IssueTag.TagColor("#ffffff", "#ff7bc3"), "c13" to IssueTag.TagColor("#444444", "#fed74a"),
                "c14" to IssueTag.TagColor("#444444", "#b7e281"), "c15" to IssueTag.TagColor("#45818e", "#d8f7f3"),
                "c16" to IssueTag.TagColor("#888888", "#e6e6e6"), "c17" to IssueTag.TagColor("#4da400", "#e6f6cf"),
                "c18" to IssueTag.TagColor("#b45f06", "#ffee9c"), "c19" to IssueTag.TagColor("#444444", "#ffc8ea"),
                "c20" to IssueTag.TagColor("#ffffff", "#e30000"), "c21" to IssueTag.TagColor("#3d85c6", "#e0f1fb"),
                "c22" to IssueTag.TagColor("#dc5766", "#fce5f1"), "c23" to IssueTag.TagColor("#b45f06", "#f7e9c1"),
                "c24" to IssueTag.TagColor("#444444", "#92e1d5"), "c25" to IssueTag.TagColor("#444444", "#a6e0fc"),
                "c26" to IssueTag.TagColor("#444444", "#e0c378"), "c27" to IssueTag.TagColor("#444444", "#bababa"),
                "c28" to IssueTag.TagColor("#ffffff", "#25beb2"), "c29" to IssueTag.TagColor("#ffffff", "#42a3df"),
                "c30" to IssueTag.TagColor("#ffffff", "#878787"), "c31" to IssueTag.TagColor("#ffffff", "#4d4d4d"),
                "c32" to IssueTag.TagColor("#ffffff", "#246512"), "c33" to IssueTag.TagColor("#ffffff", "#00665e"),
                "c34" to IssueTag.TagColor("#ffffff", "#553000"), "c35" to IssueTag.TagColor("#ffffff", "#1a1a1a")
        )
    }

    class TagColor(foreground: String, background: String) {
        val foreground: Color = Color.decode(foreground)
        val background: Color = Color.decode(background)
    }
}