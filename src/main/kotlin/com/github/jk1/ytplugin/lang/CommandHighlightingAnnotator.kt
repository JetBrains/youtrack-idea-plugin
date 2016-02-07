package com.github.jk1.ytplugin.lang

import com.github.jk1.ytplugin.components.CommandComponent
import com.github.jk1.ytplugin.model.CommandAssistResponse
import com.github.jk1.ytplugin.model.CommandHighlightRange
import com.github.jk1.ytplugin.model.YouTrackCommand
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiFile

class CommandHighlightingAnnotator : ExternalAnnotator<CommandAssistResponse, List<CommandHighlightRange>>() {

    private val LOG = Logger.getInstance(CommandHighlightingAnnotator::class.java)
    private val TEXT_ATTRIBUTES = mapOf<String, TextAttributes>(
            "field" to DefaultLanguageHighlighterColors.CONSTANT.defaultAttributes,
            "keyword" to DefaultLanguageHighlighterColors.KEYWORD.defaultAttributes,
            "string"  to DefaultLanguageHighlighterColors.STRING.defaultAttributes,
            "error" to HighlighterColors.BAD_CHARACTER.defaultAttributes
    )

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): CommandAssistResponse {
        val component: CommandComponent = file.getUserData(CommandComponent.USER_DATA_KEY) ?:
                throw IllegalStateException("Command component user data is missing from PSI file")
        return component.suggest(YouTrackCommand(file.text, editor.caretModel.offset))
    }

    override fun doAnnotate(collectedInfo: CommandAssistResponse): List<CommandHighlightRange> {
        return collectedInfo.highlightRanges
    }

    override fun apply(file: PsiFile, ranges: List<CommandHighlightRange>, holder: AnnotationHolder) {
        ranges.forEach {
            if ("error".equals(it.styleClass)) {
                holder.createErrorAnnotation(it.getTextRange(), null)
            } else {
                val info = holder.createInfoAnnotation(it.getTextRange(), null)
                val attributes = TEXT_ATTRIBUTES[it.styleClass] ?: HighlighterColors.TEXT.defaultAttributes
                info.enforcedTextAttributes = attributes
            }
        }
    }
}