package com.github.jk1.ytplugin.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.Divider
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.ClickListener
import org.jetbrains.annotations.NotNull
import java.awt.Cursor
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.CENTER
import java.awt.GridBagConstraints.NONE
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JLabel

class EditorSplitter(vertical: Boolean) : Splitter(vertical) {

    private var previewCollapsed = true

    init {
        isShowDividerControls = true
        isShowDividerIcon = true
        dividerWidth = 10
        collapse()
    }

    fun collapse() {
        proportion = 1.0f - getMinProportion(false)
        previewCollapsed = true
    }

    fun expand() {
        proportion = 0.5f
        previewCollapsed = false
    }

    override fun createDivider(): Divider {
        return object : Divider(GridBagLayout()) {

            override fun setSwitchOrientationEnabled(switchOrientationEnabled: Boolean) {
            }

            override fun setResizeEnabled(resizeEnabled: Boolean) {
            }

            override fun setOrientation(ignored: Boolean) {
                removeAll()
                val collapseLabel = createLabel(AllIcons.General.ArrowRight)
                val expandLabel = createLabel(AllIcons.General.ArrowLeft)
                add(collapseLabel, GridBagConstraints(0, 5, 1, 1, .0, .0, CENTER, NONE, Insets(0, 0, 0, 0), 0, 0))
                add(expandLabel, GridBagConstraints(0, 1, 1, 1, .0, .0, CENTER, NONE, Insets(0, 0, 0, 0), 0, 0))
                revalidate()
                repaint()
            }

            private fun createLabel(icon: Icon): JLabel {
                val label = JLabel(icon)
                label.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                clickListener().installOn(label)
                return label
            }

            private fun clickListener() = object : ClickListener() {
                override fun onClick(@NotNull event: MouseEvent, clickCount: Int): Boolean {
                    if (previewCollapsed) {
                        expand()
                    } else {
                        collapse()
                    }
                    return true
                }
            }
        }
    }


}