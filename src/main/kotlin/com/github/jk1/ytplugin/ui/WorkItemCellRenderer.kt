package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.format
import com.github.jk1.ytplugin.issues.model.IssueWorkItem
import com.intellij.icons.AllIcons
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Font

class WorkItemCellRenderer(val workItem: IssueWorkItem) {

    private val complimentaryColor = Color(123, 123, 127)
    private val idStyle = SimpleTextAttributes.STYLE_PLAIN

    fun fillDateComponent(): SimpleColoredComponent {
        val date = SimpleColoredComponent()
        date.isOpaque = false
        date.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        date.append(workItem.date.format().substring(0, workItem.date.format().length - 5),
            SimpleTextAttributes(idStyle, complimentaryColor)
        )
        return date
    }

    fun fillIssueComponent(): SimpleColoredComponent {
        val value = SimpleColoredComponent()
        value.isOpaque = false
        value.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        value.icon = AllIcons.Vcs.History
        value.append(workItem.value, SimpleTextAttributes(idStyle, complimentaryColor))
        return value
    }

    fun fillTypeComponent(): SimpleColoredComponent {
        val type = SimpleColoredComponent()
        type.isOpaque = false
        type.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        type.append(if (workItem.type == "None") "No type" else workItem.type,
            SimpleTextAttributes(idStyle, complimentaryColor))
        return type
    }

    fun fillAttributesComponents(): List<SimpleColoredComponent> {
        return workItem.attributes.map {
            val attribute = SimpleColoredComponent()
            attribute.isOpaque = false
            attribute.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
            attribute.append ("${it.name}: ${it.value!!}", SimpleTextAttributes(idStyle, complimentaryColor))
            attribute
        }
    }

    fun fillCommentComponent(viewportWidth: Int): SimpleColoredComponent {
        val trackingComments = SimpleColoredComponent()
        trackingComments.clear()
        trackingComments.font = Font(UIUtil.getLabelFont().family, Font.PLAIN, UIUtil.getLabelFont().size + 1)
        trackingComments.isOpaque = false

        val comments = workItem.comment?.split(" ")?.iterator()
        if (comments != null && workItem.comment != "") {
            trackingComments.icon = AllIcons.General.Balloon

            while (comments.hasNext() && (viewportWidth > trackingComments.computePreferredSize(false).width)) {
                trackingComments.append(" ${comments.next()}", SimpleTextAttributes(idStyle, complimentaryColor))
            }
        }
        if (comments != null && workItem.comment != "") {
            if (comments.hasNext()) {
                trackingComments.append(" …", SimpleTextAttributes(idStyle, complimentaryColor))
            }
        }
        return trackingComments
    }

}