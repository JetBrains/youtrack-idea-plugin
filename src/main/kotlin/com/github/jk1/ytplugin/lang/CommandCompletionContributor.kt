package com.github.jk1.ytplugin.lang

import com.github.jk1.ytplugin.components.CommandComponent
import com.github.jk1.ytplugin.model.CommandSuggestion
import com.github.jk1.ytplugin.model.YouTrackCommand
import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.impl.DebugUtil
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class CommandCompletionContributor : CompletionContributor() {

    final val LOG = Logger.getInstance(CommandCompletionContributor::class.java)
    final val TIMEOUT = 2000L // ms

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (LOG.isDebugEnabled) {
            LOG.debug(DebugUtil.psiToString(parameters.originalFile, true))
        }
        super.fillCompletionVariants(parameters, result)
        val file = parameters.originalFile
        val component : CommandComponent = file.getUserData(CommandComponent.USER_DATA_KEY) ?: return
        val future = ApplicationManager.getApplication().executeOnPooledThread (
                Callable<List<CommandSuggestion>> {
                    val command =YouTrackCommand(parameters.originalFile.text, parameters.offset)
                    component.suggest(command).suggestions
                })
        try {
            val suggestions: List<CommandSuggestion> = future.get(TIMEOUT, TimeUnit.MILLISECONDS)
            // actually backed by original CompletionResultSet
            val result = result.withPrefixMatcher(extractPrefix(parameters)).caseInsensitive()
            result.addAllElements(suggestions.map {
                LookupElementBuilder.create(it, it.option)
                        .withTypeText(it.description, true)
                        .withInsertHandler(MyInsertHandler)
                        .withBoldness(it.styleClass.equals("keyword"))
            })
        } catch (ignored: TimeoutException) {
            LOG.debug("YouTrack request took more than $TIMEOUT ms to complete")
        } catch (e: Exception) {
            LOG.debug(e)
        }
    }

    /**
     * Find first word left boundary before cursor and strip leading braces and '#' signs
     */
    fun extractPrefix(parameters: CompletionParameters): String {
        val text = parameters.originalFile.text
        val caretOffset = parameters.offset
        if (text.isEmpty() || caretOffset == 0) {
            return ""
        }
        var stopAt = text.lastIndexOf('{', caretOffset - 1)
        // caret isn't inside braces
        if (stopAt <= text.lastIndexOf('}', caretOffset - 1)) {
            // we stay right after colon
            if (text.get(caretOffset - 1) == ':') {
                stopAt = caretOffset - 1
            }
            // use rightmost word boundary as last resort
            else {
                stopAt = text.lastIndexOf(' ', caretOffset - 1)
            }
        }
        //int start = CharArrayUtil.shiftForward(text, lastSpace < 0 ? 0 : lastSpace + 1, "#{ ")
        var prefixStart = stopAt + 1
        if (prefixStart < caretOffset && text[prefixStart] == '#') {
            prefixStart++
        }
        return StringUtil.trimLeading(text.substring(prefixStart, caretOffset))
    }

    /**
     * Inserts additional braces around values that contains spaces, colon after attribute names
     * and '#' before short-cut attributes if any
     */
    object MyInsertHandler : InsertHandler<LookupElement> {

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
}
