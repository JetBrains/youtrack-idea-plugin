package com.github.jk1.ytplugin.model

import com.intellij.openapi.util.TextRange
import org.jdom.Element

public class CommandSuggestion(item: Element) {

    val matchRange: TextRange
    val completionRange: TextRange
    val caretPosition: Int
    val description: String
    val suffix: String
    val prefix: String
    val option: String
    val styleClass: String

    init {
        matchRange = TextRange(
                Integer.parseInt(item.getChild("match").getAttributeValue("start")),
                Integer.parseInt(item.getChild("match").getAttributeValue("end"))
        )
        completionRange = TextRange(
                Integer.parseInt(item.getChild("completion").getAttributeValue("start")),
                Integer.parseInt(item.getChild("completion").getAttributeValue("end"))
        )
        description = item.getChildText("description")
        option = item.getChildText("option")
        suffix = item.getChildText("suffix") ?: ""
        prefix = item.getChildText("prefix") ?: ""
        styleClass = item.getChildText("styleClass")
        caretPosition = Integer.valueOf(item.getChildText("caret"))
    }

   /* public fun asLookupElement(){
        LookupElementBuilder.create(this, option)
                .withTypeText(it.description, true)
                .withInsertHandler(CommandCompletionContributor.MyInsertHandler)
                .withBoldness(it.styleClass.equals("keyword"))
    }*/
}
