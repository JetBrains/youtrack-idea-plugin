package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import java.awt.Font
import java.util.concurrent.TimeUnit
import javax.swing.JLabel
import javax.swing.Timer

class TimerWidget(val timeTracker: TimeTracker, private val parentDisposable: Disposable, override val project: Project) : CustomStatusBarWidget, ComponentAware {

    private val label = JLabel(time())
    private val timer = Timer(1000) { label.text = time() }

    private val logsTimer = Timer(60000) {
        logger.debug(
            "\nSystem.currentTimeMillis: ${System.currentTimeMillis()} \n" +
                    "timeTracker.startTime: ${timeTracker.startTime} \n" +
                    "timeTracker.pausedTime: ${timeTracker.pausedTime} \n")
    }

    private var trackingDisposable: Disposable? = null


    fun time(): String {
        val savedTime = spentTimePerTaskStorage.getSavedTimeForLocalTask(taskManagerComponent.getActiveTask().id)
        val recordedTime = if (timeTracker.isPaused) {
            timeTracker.getRecordedTimeInMills() + savedTime
        } else {
            System.currentTimeMillis() - timeTracker.startTime - timeTracker.pausedTime + savedTime
        }

        logger.debug("\nsavedTime: $savedTime \nrecordedTime: $recordedTime \n \n")

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
        logsTimer.start()
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