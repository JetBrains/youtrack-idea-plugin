package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.WindowManager
import java.util.concurrent.TimeUnit


class TimeTracker() {

    var issueId: String = "Default"
    private var time: String = ""
    private var comment: String = ""

    private var startTime: Long = 0
    var isRunning = false

    fun stop(project: Project): String {
        return if (isRunning){
            logger.debug("Time tracking stopped")
            time = formatTimePeriod((System.currentTimeMillis() - startTime))
            isRunning = false
            val bar = WindowManager.getInstance().getStatusBar(project)
            bar?.removeWidget("Time Tracking Clock")
            time
        }
        else{
            TrackerNotifier.infoBox("Could not stop time tracking - timer is not started", "");
            logger.debug("Time tracking was not recorded")
            "0"
        }
    }

    fun start(project: Project){
        if (!isRunning) {
            logger.debug("Time tracking started for issue $issueId")
            TrackerNotifier.infoBox("Time tracking started for issue $issueId", "");

            startTime = System.currentTimeMillis()
            isRunning = true
            val bar = WindowManager.getInstance().getStatusBar(project)
            bar?.addWidget(ClockWidget(startTime))
        }
    }

    private fun formatTimePeriod(timeInMilSec: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMilSec)

        return if (minutes > 0){
            TrackerNotifier.infoBox("Time tracking stopped", "");
            minutes.toString()
        }
        else{
            TrackerNotifier.infoBox("Time is not recorded (< 1min)", "");
            "0"
        }

    }

    fun getRecordedTime() = time

    fun getComment() = comment

}


