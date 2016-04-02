package com.github.jk1.ytplugin.commands.lang

import com.github.jk1.ytplugin.commands.model.CommandSuggestion
import com.intellij.codeInsight.completion.InsertHandler
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElement

/**
 * Inserts additional braces around values that contains spaces, colon after attribute names
 * and '#' before short-cut attributes if any
 */
object CommandSuggestionInsertHandler: InsertHandler<LookupElement> {

    override fun handleInsert(context: InsertionContext, item: LookupElement) {
        val completionItem = item.`object` as CommandSuggestion
        val document = context.document
        val editor = context.editor

        context.commitDocument()
        context.setAddCompletionChar(false)

        val prefix = completionItem.prefix
        val suffix = completionItem.suffix
        var text = document.text
        var offset = context.startOffset
        // skip possible spaces after '{', e.g. "{  My Project <caret>"
        if (prefix.endsWith("{")) {
            while (offset > prefix.length && Character.isWhitespace(text[offset - 1])) {
                offset--
            }
        }
        if (!prefix.isEmpty() && !hasPrefixAt(document.text, offset - prefix.length, prefix)) {
            document.insertString(offset, prefix)
        }
        offset = context.tailOffset
        text = document.text
        if (suffix.startsWith("} ")) {
            while (offset < text.length - suffix.length && Character.isWhitespace(text[offset])) {
                offset++
            }
        }
        if (!suffix.isEmpty() && !hasPrefixAt(text, offset, suffix)) {
            document.insertString(offset, suffix)
        }
        editor.caretModel.moveToOffset(context.tailOffset)
    }

    fun hasPrefixAt(text: String, offset: Int, prefix: String): Boolean {
        if (text.isEmpty() || offset < 0 || offset >= text.length) {
            return false
        }
        return text.regionMatches(offset, prefix, 0, prefix.length, true)
    }
}