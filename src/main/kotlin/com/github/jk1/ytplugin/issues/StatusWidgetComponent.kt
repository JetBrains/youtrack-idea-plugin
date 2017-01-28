package com.github.jk1.ytplugin.issues

import com.intellij.openapi.components.AbstractProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.WindowManager
import java.awt.Component
import javax.swing.SwingUtilities

/**
 * Status widget is not used yet, we just have nothing meaningful to display.
 * todo: plug it in when notification support will be implemented
 */
class StatusWidgetComponent(val project: Project) : AbstractProjectComponent(project) {

    @Volatile var offline = false
    set(value) {
        field = value
        SwingUtilities.invokeLater { WindowManager.getInstance().getStatusBar(project).updateWidget(widget.ID()) }
    }

    private val widget = object : StatusBarWidget {
        override fun ID() = "YouTrack Plugin Status Widget"

        override fun getPresentation(type: StatusBarWidget.PlatformType) = object : StatusBarWidget.TextPresentation {
            override fun getText() = if (offline) " YouTrack is offline " else ""
            override fun getTooltipText() = "Last request to YouTrack server has returned an error"
            override fun getMaxPossibleText() = " YouTrack is offline "
            override fun getAlignment() = Component.CENTER_ALIGNMENT
            override fun getClickConsumer() = null
        }

        override fun install(statusBar: StatusBar) {
        }

        override fun dispose() {
            Disposer.dispose(this)
        }
    }

    override fun projectOpened() {
        WindowManager.getInstance().getStatusBar(project).addWidget(widget)
    }
}