package com.github.jk1.ytplugin.timeTracker

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import java.awt.event.ActionListener
import java.util.concurrent.TimeUnit
import javax.swing.JLabel
import javax.swing.Timer
import kotlin.jvm.internal.Intrinsics

class TimerWidget(val timeTracker: TimeTracker, private val parentDisposable: Disposable) : CustomStatusBarWidget {

    val label = JLabel(time())
    private val timer = Timer(1000, ActionListener { label.text = time() })
    private var trackingDisposable: Disposable? = null


    fun time(): String {
        val recordedTime = if (timeTracker.isPaused){
            timeTracker.getRecordedTimeInMills()
        } else {
            timeTracker.getRecordedTimeInMills() + System.currentTimeMillis() - timeTracker.startTime
        }
        val time = String.format("%02d:%02d",
        TimeUnit.MILLISECONDS.toHours(recordedTime),
        TimeUnit.MILLISECONDS.toMinutes(recordedTime) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(recordedTime)))
        return "Time spent: $time"
    }

    override fun install(statusBar: StatusBar) {
        label.text = "Time spent: 00:00"
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