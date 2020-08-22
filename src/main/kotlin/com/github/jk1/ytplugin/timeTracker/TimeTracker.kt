package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import java.util.concurrent.TimeUnit


class TimeTracker() {

    var issueId: String = "Default"
    private var time: String = ""
    private var comment: String = ""

    private var startTime: Long = 0
    var isRunning = false

    fun stop(project: Project, idReadable: String): String {
        val trackerNote = TrackerNotification()

        return if (isRunning){
            time = formatTimePeriod((System.currentTimeMillis() - startTime))
            isRunning = false

            val bar = WindowManager.getInstance().getStatusBar(project)
            bar?.removeWidget("Time Tracking Clock")

            time
        } else {
            trackerNote.notify("Could not stop time tracking: timer is not started")
            "0"
        }
    }

    fun start(project: Project, idReadable: String){
        if (!isRunning) {

            val trackerNote = TrackerNotification()
            trackerNote.notify("Work timer started for Issue $idReadable")

            startTime = System.currentTimeMillis()
            isRunning = true
            val bar = WindowManager.getInstance().getStatusBar(project)
            bar?.addWidget(ClockWidget(startTime))
        }
    }

    private fun formatTimePeriod(timeInMilSec: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMilSec)
        return if (minutes > 0)
            minutes.toString()
        else
            "0"
    }

    fun getRecordedTime() = time

    fun getComment() = comment

}


