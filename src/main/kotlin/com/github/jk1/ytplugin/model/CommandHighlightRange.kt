package com.github.jk1.ytplugin.model

import com.intellij.openapi.util.TextRange
import org.jdom.Element


class CommandHighlightRange(rangeElement: Element) {

    val start: Int
    val end: Int
    val styleClass: String

    init {
        start = Integer.valueOf(rangeElement.getChildText("start"))
        end = Integer.valueOf(rangeElement.getChildText("end"))
        styleClass = rangeElement.getChildText("styleClass") ?: "string"
    }

    fun getRange() = TextRange(start, end)

    fun getTextRange() = TextRange.create(start, end)
}