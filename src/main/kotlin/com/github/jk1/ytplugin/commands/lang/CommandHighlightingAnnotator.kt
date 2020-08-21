package com.github.jk1.ytplugin.commands.lang

import com.github.jk1.ytplugin.commands.CommandService
import com.github.jk1.ytplugin.commands.CommandService.Companion.SERVICE_KEY
import com.github.jk1.ytplugin.commands.ICommandService
import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.CommandHighlightRange
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.HighlighterColors
import com.intellij.openapi.editor.HighlighterColors.TEXT
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.psi.PsiFile

/**
 * Highlights YouTrack command based on the server-residing command parser's response.
 * It's main duty is to map highlight range classes and to compute necessary text
 * attributes to display the command.
 */
class CommandHighlightingAnnotator : ExternalAnnotator<CommandAssistResponse, List<CommandHighlightRange>>() {

    companion object {
        private val TEXT_ATTRIBUTES = mapOf<String, TextAttributes>(
                "field" to DefaultLanguageHighlighterColors.CONSTANT.defaultAttributes,
                "keyword" to DefaultLanguageHighlighterColors.KEYWORD.defaultAttributes,
                "string" to DefaultLanguageHighlighterColors.STRING.defaultAttributes,
                "error" to HighlighterColors.BAD_CHARACTER.defaultAttributes
        )
    }

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): CommandAssistResponse {
        val component: ICommandService = file.getUserData(SERVICE_KEY)
                ?: throw IllegalStateException("Command component user data is missing from the PSI file")
        val session = file.getUserData(CommandService.ISSUE_KEY)
                ?: throw IllegalStateException("Command component user data is missing from the PSI file")
        return component.suggest(YouTrackCommand(session, file.text, editor.caretModel.offset))
    }

    override fun doAnnotate(collectedInfo: CommandAssistResponse): List<CommandHighlightRange> {
        return collectedInfo.highlightRanges
    }

    override fun apply(file: PsiFile, ranges: List<CommandHighlightRange>, holder: AnnotationHolder) {
        ranges.forEach {
            if ("error" == it.styleClass) {
                holder.newSilentAnnotation(HighlightSeverity.ERROR).range(it.getTextRange()).create()
            } else {
                holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                        .range(it.getTextRange())
                        .enforcedTextAttributes(TEXT_ATTRIBUTES[it.styleClass] ?: TEXT.defaultAttributes)
                        .create()
            }
        }
    }
}