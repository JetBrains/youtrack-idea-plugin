package com.github.jk1.ytplugin.ui

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.issues.model.CustomField
import com.github.jk1.ytplugin.issues.model.Issue
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import java.awt.Dimension
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.SwingConstants


class IssueListCellIconProvider(override val project: Project) : ComponentAware {

    // reference size to align all compact view icons with
    val compactLabelDimension: Dimension

    init {
        val label = JLabel(" Z ")
        label.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        compactLabelDimension = label.preferredSize
    }

    fun createIcon(issue: Issue, compact: Boolean) = if (compact) createCompactIcon(issue) else createIcon(issue)

    fun createIcon(issue: Issue): JComponent {
        if (isActive(issue)) {
            val label = JLabel(AllIcons.Toolwindows.ToolWindowFavorites, SwingConstants.CENTER)
            label.border = BorderFactory.createEmptyBorder(0, 5, 0, 0)
            return label
        } else {
            val label = JLabel()
            label.border = BorderFactory.createEmptyBorder(0, 2, 0, 0)
            return label
        }
    }

    fun createCompactIcon(issue: Issue): JComponent {
        val coloredField = getColoredField(issue)
        if (coloredField == null) {
            return createIcon(issue)
        } else {
            val label = when (isActive(issue)) {
                true -> JLabel(AllIcons.Toolwindows.ToolWindowFavorites, SwingConstants.CENTER)
                false -> JLabel(" ${coloredField.value.first().first()} ")
            }
            with(label) {
                background = coloredField.backgroundColor
                foreground = coloredField.foregroundColor
                font = Font(Font.MONOSPACED, Font.PLAIN, 12)
                isOpaque = !UIUtil.isUnderDarcula()
                preferredSize = compactLabelDimension
            }
            return label
        }
    }

    private fun getColoredField(issue: Issue): CustomField? {
        val coloredFields = issue.customFields.filter { it.backgroundColor != null && it.foregroundColor != null }
        val priorityField = coloredFields.firstOrNull { "Priority" == it.name }
        return priorityField ?: coloredFields.firstOrNull()
    }


    private fun isActive(issue: Issue) = (taskManagerComponent.getActiveTask().id == issue.id)
}