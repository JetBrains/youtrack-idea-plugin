package com.github.jk1.ytplugin.commands.lang

import com.github.jk1.ytplugin.commands.components.CommandComponent
import com.github.jk1.ytplugin.commands.components.CommandComponent.Companion.COMPONENT_KEY
import com.github.jk1.ytplugin.commands.components.CommandComponent.Companion.SESSION_KEY
import com.github.jk1.ytplugin.commands.model.CommandSuggestion
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.impl.DebugUtil
import java.util.concurrent.Callable
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Provides smart completion for YouTrack command language.
 */
class CommandCompletionContributor : CompletionContributor() {

    val LOG = Logger.getInstance(CommandCompletionContributor::class.java)
    val TIMEOUT = 2000L // ms

    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        if (LOG.isDebugEnabled) {
            LOG.debug(DebugUtil.psiToString(parameters.originalFile, true))
        }
        super.fillCompletionVariants(parameters, result)
        val file = parameters.originalFile
        val component = file.getUserData(COMPONENT_KEY) ?: return
        val session = file.getUserData(SESSION_KEY) ?: return
        val future = ApplicationManager.getApplication().executeOnPooledThread (
                Callable<List<CommandSuggestion>> {
                    val command = YouTrackCommand(session, parameters.originalFile.text, parameters.offset)
                    component.suggest(command).suggestions
                })
        try {
            val suggestions: List<CommandSuggestion> = future.get(TIMEOUT, TimeUnit.MILLISECONDS)
            // actually backed by original CompletionResultSet
            result.withPrefixMatcher(extractPrefix(parameters))
                    .caseInsensitive()
                    .addAllElements(createLookupElements(suggestions))
        } catch (e: TimeoutException) {
            LOG.debug("YouTrack request took more than $TIMEOUT ms to complete")
        } catch (e: Exception) {
            LOG.warn(e)
        }
    }


    fun createLookupElements(suggestions: List<CommandSuggestion>): Iterable<LookupElement> {
        /**
         * |Bug             Type|     |Bug                  Type|
         * |Feature         Type|     |Feature              Type|
         * |Recent commands-----|  => |#Fixed    Recent commands|
         * |#Fixed        20 Feb|     |#Reopen   Recent commands|
         * |#Reopen       22 Feb|
         */
        val separatorIndex = suggestions.indexOfFirst { it.separator }
        return suggestions.filter { !it.separator }.mapIndexed { index, suggestion ->
            val typeText = when {
                separatorIndex == -1 -> suggestion.description
                index < separatorIndex -> suggestion.description
                else -> suggestions[separatorIndex].description
            }
            val element = LookupElementBuilder.create(suggestion, suggestion.option)
                    .withTypeText(typeText, true)
                    .withInsertHandler(CommandSuggestionInsertHandler)
            // override default completion sorting with an explicit order from the server
            PrioritizedLookupElement.withPriority(element, -index.toDouble())
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
            if (text[caretOffset - 1] == ':') {
                stopAt = caretOffset - 1
            }
            // use rightmost word boundary as last resort
            else {
                stopAt = text.lastIndexOf(' ', caretOffset - 1)
            }
        }
        var prefixStart = stopAt + 1
        if (prefixStart < caretOffset && text[prefixStart] == '#') {
            prefixStart++
        }
        return StringUtil.trimLeading(text.substring(prefixStart, caretOffset))
    }
}
