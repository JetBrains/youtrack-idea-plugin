package com.github.jk1.ytplugin.search

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.ui.JBSplitter
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.SwingUtilities

/**
 * Created by elle on 03.04.16.
 */
class EditorSplitter(val project: Project) : JBSplitter(false) {
    val WIDTH_PROPERTY_NAME = "MyyMessagesListWidth"
    val myMessagesWidth = PropertiesComponent.getInstance(project).getOrInitLong(WIDTH_PROPERTY_NAME, 350).toInt()

    override fun addNotify() {
        super.addNotify()
        SwingUtilities.invokeLater {
            setProportion(myMessagesWidth.toFloat() / getWidth().toFloat())
            getFirstComponent().addComponentListener(object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent?) {
                    PropertiesComponent.getInstance(project).setValue(WIDTH_PROPERTY_NAME, Math.max(350, e!!.component.width).toString())
                }
            })
        }
    }
}