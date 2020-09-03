package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.tasks.TaskManager
import java.util.concurrent.TimeUnit


class TimeTracker() {

//    override val project = myProject

//    val repo = project.let { it1 -> ComponentAware.of(it1).taskManagerComponent.getActiveYouTrackRepository() }

    var issueId: String = "Default"
    var inactivityPeriodInMills: Long = 0
    var type: String = "None"
    var isManualTrackingEnable = true
    var scheduledPeriod: Long = 0
    private var time: String = ""
    private var comment: String = "default comment"
    private var timeInMills: Long = 0
    var isAutoTrackingEnable = false
    private var startTime: Long = 0
    var isRunning = false
    var isPaused = false
    var activityTracker: ActivityTracker? = null

    fun stop(): String {
        val trackerNote = TrackerNotification()

        if (isAutoTrackingEnable){
            startTime = activityTracker?.startInactivityTime!!
        }

        return if (isRunning) {
            if (!isPaused) {
                timeInMills += (System.currentTimeMillis() - startTime)
            }
            time = formatTimePeriod(timeInMills)
            timeInMills = 0
            isRunning = false
            isPaused = false

            time
        } else {
            trackerNote.notify("Could not stop time tracking: timer is not started", NotificationType.ERROR)
            "0"
        }
    }


    fun pause() {
        val trackerNote = TrackerNotification()
        if (isPaused) {
            trackerNote.notify("Timer already paused", NotificationType.ERROR)
        } else {
            if (isRunning) {
                timeInMills += (System.currentTimeMillis() - startTime)
                isPaused = true
                trackerNote.notify("Work timer paused", NotificationType.INFORMATION)
            } else {
                trackerNote.notify("Could not pause - timer is not started", NotificationType.ERROR)
            }
        }
    }

    fun start(idReadable: String, repo: YouTrackServer, project: Project, taskManager: TaskManager) {
        val trackerNote = TrackerNotification()
        trackerNote.notify("Work timer started for Issue $idReadable", NotificationType.INFORMATION)
        startTime = System.currentTimeMillis()
        isRunning = true
        isPaused = false
    }

    private fun formatTimePeriod(timeInMilSec: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMilSec)
        return if (minutes > 0)
            minutes.toString()
        else
            "0"
    }

    fun getRecordedTime() = time

    fun getRecordedTimeInMills() = timeInMills

    fun getStartTime() = startTime

    fun getComment() = comment

}


