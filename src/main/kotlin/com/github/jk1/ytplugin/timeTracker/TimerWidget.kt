package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import java.awt.Font
import java.awt.event.ActionListener
import java.util.concurrent.TimeUnit
import javax.swing.JLabel
import javax.swing.Timer

class TimerWidget(val timeTracker: TimeTracker, private val parentDisposable: Disposable, override val project: Project) : CustomStatusBarWidget, ComponentAware {

    private val label = JLabel(time())
    private val timer = Timer(1000, ActionListener { label.text = time() })
    private var trackingDisposable: Disposable? = null


    fun time(): String {
        val savedTime = spentTimePerTaskStorage.getSavedTimeForLocalTask(taskManagerComponent.getActiveTask().id)
        val recordedTime = if (timeTracker.isPaused) {
            timeTracker.getRecordedTimeInMills() + savedTime
        } else {
            System.currentTimeMillis() - timeTracker.startTime - timeTracker.pausedTime + savedTime
        }
        val time = String.format("%02dh %02dm",
                TimeUnit.MILLISECONDS.toHours(recordedTime),
                TimeUnit.MILLISECONDS.toMinutes(recordedTime) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(recordedTime)))
        return "Time spent on issue ${timeTracker.issueId}: $time"
    }


    override fun install(statusBar: StatusBar) {

        val f: Font = label.font
        label.font = f.deriveFont(f.style or Font.BOLD)
        trackingDisposable = ActivityTracker.newDisposable(parentDisposable)
        timer.start()
    }

    override fun dispose() {
        if (trackingDisposable != null) {
            Disposer.dispose(trackingDisposable!!)
            trackingDisposable = null
        }
    }

    override fun getComponent(): JLabel {
        return label
    }

    override fun ID(): String {
        return "Time Tracking Clock"
    }
}