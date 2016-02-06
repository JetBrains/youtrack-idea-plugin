package com.github.jk1.ytplugin.lang

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.ExternalAnnotator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.tasks.youtrack.YouTrackIntellisense
import java.util.*

class CommandHighlightingAnnotator : ExternalAnnotator<QueryInfo, List<YouTrackIntellisense.HighlightRange>>() {

    private val LOG = Logger.getInstance(CommandHighlightingAnnotator::class.java)

    override fun collectInformation(file: PsiFile, editor: Editor, hasErrors: Boolean): QueryInfo? {
        val intellisense = file.getUserData(YouTrackIntellisense.INTELLISENSE_KEY);
        if (intellisense == null || !intellisense.repository.isConfigured) {
            return null;
        }
        return QueryInfo(editor.caretModel.offset, file.text, intellisense);
    }

    override fun doAnnotate(collectedInfo: QueryInfo?): List<YouTrackIntellisense.HighlightRange> {
        try {
            return collectedInfo?.fetchHighlighting() ?: Collections.emptyList()
        } catch (e: Exception) {
            return Collections.emptyList()
        }
    }

    override fun apply(file: PsiFile, ranges: List<YouTrackIntellisense.HighlightRange>, holder: AnnotationHolder) {
        ranges.forEach {
            if("error".equals(it.styleClass)){
                holder.createErrorAnnotation(it.textRange, null)
            } else {
                val info = holder.createInfoAnnotation(it.textRange, null)
                info.enforcedTextAttributes = it.textAttributes
            }
        }
    }
}

data class QueryInfo(val caretOffset: Int, val text: String, val intellisense: YouTrackIntellisense) {

    fun fetchHighlighting(): List<YouTrackIntellisense.HighlightRange> {
        return intellisense.fetchHighlighting(text, caretOffset)
    }
}