package com.github.jk1.ytplugin.model

import org.jdom.input.SAXBuilder
import java.io.InputStream

/**
 * Main wrapper around "IntelliSense" element in YouTrack response. It delegates further parsing
 * to CommandHighlightRange and CommandSuggestion classes
 */
class CommandAssistResponse(stream: InputStream) {

    val highlightRanges: List<CommandHighlightRange>
    val suggestions: List<CommandSuggestion>

    init {
        val root = SAXBuilder().build(stream).rootElement
        highlightRanges = root.getChild("highlight").getChildren("range").map { CommandHighlightRange(it) }
        suggestions = root.getChild("suggest").getChildren("item").map { CommandSuggestion(it) }
    }
}