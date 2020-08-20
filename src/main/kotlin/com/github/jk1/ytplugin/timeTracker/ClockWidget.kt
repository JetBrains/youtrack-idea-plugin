package com.github.jk1.ytplugin.timeTracker

import com.intellij.openapi.wm.CustomStatusBarWidget
import com.intellij.openapi.wm.StatusBar
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.JLabel
import javax.swing.Timer
import kotlin.jvm.internal.Intrinsics

class ClockWidget(startTime: Long) : CustomStatusBarWidget {
    val label = JLabel(time())

    private val startingTime = startTime
    private val timer = Timer(1000, ActionListener { label.text = time() })

    fun time(): String {
        val recordedTime = System.currentTimeMillis() - startingTime

        val time = String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(recordedTime),
                TimeUnit.MILLISECONDS.toMinutes(recordedTime) -
                        TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(recordedTime)))

        return "Time spent: $time"
    }

    override fun install(statusBar: StatusBar) {
        Intrinsics.checkParameterIsNotNull(statusBar, "statusBar")
        label.text = "Time spent: 00:00"
        timer.start()
    }

    override fun dispose() {
        timer.stop()
    }

    override fun getComponent(): JLabel {
        return label
    }

    override fun ID(): String {
        return "Time Tracking Clock"
    }
}