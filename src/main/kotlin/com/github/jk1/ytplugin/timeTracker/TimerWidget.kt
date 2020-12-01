package com.github.jk1.ytplugin.timeTracker

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import java.awt.Font
import java.awt.event.ActionListener
import java.util.concurrent.TimeUnit
import javax.swing.JLabel
import javax.swing.Timer

class TimerWidget(val timeTracker: TimeTracker, private val parentDisposable: Disposable) : CustomStatusBarWidget {

    val label = JLabel(time())
    private val timer = Timer(1000, ActionListener { label.text = time() })
    private var trackingDisposable: Disposable? = null


    fun time(): String {
        val recordedTime = if (timeTracker.isPaused) {
            timeTracker.getRecordedTimeInMills()
        } else {
            System.currentTimeMillis() - timeTracker.startTime - timeTracker.pausedTime
        }
        val time = String.format("%02dh %02dm",
                TimeUnit.MILLISECONDS.toHours(recordedTime),
                TimeUnit.MILLISECONDS.toMinutes(recordedTime) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(recordedTime)))
        return mode() + "Time spent: $time"
    }

    private fun mode(): String {
        return if (timeTracker.isAutoTrackingEnable && timeTracker.isManualTrackingEnable)
            "Automatic and Manual tracking mode is on. "
        else if (timeTracker.isAutoTrackingEnable)
            "Automatic tracking mode is on. "
        else
            "Manual tracking mode is on. "
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