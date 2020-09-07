package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.TimeUnit

@Service
class TimeTracker(override val project: Project) : ComponentAware{

    var issueId: String = "Default"
    var inactivityPeriodInMills: Long = 600000
    var type: String = "None"
    var scheduledPeriod: Long = 0
    var recordedTime: String = ""
    var timeInMills: Long = 0
    var startTime: Long = 0
    var comment: String = "default comment"
    var isManualTrackingEnable = true
    var isScheduledUnabled = true
    var isWhenProjectClosedUnabled = true
    var isPostAfterCommitUnabled = false
    var isAutoTrackingEnable = true
    var isRunning = false
    var isPaused = false
    var activityTracker: ActivityTracker? = null

//    operator fun get(repo: YouTrackServer): TimeTracker {
//        logger.debug("Get time tracker for project")
//        return this
//    }

    private val listeners: MutableSet<() -> Unit> = mutableSetOf()


    fun subscribe(listener: () -> Unit) {
        listeners.add(listener)
    }

    fun stop(): String {
        val trackerNote = TrackerNotification()

        return if (isRunning) {
            if (!isPaused) {
                timeInMills += (System.currentTimeMillis() - startTime)
            }
            recordedTime = formatTimePeriod(timeInMills)
            timeInMills = 0
            isRunning = false
            isPaused = false
            recordedTime
        } else {
            trackerNote.notify("Could not stop time tracking: timer is not started", NotificationType.ERROR)
            "0"
        }
    }


    fun pause() {
        if (isAutoTrackingEnable){
            startTime = activityTracker?.startInactivityTime!!
        }

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

    fun start(idReadable: String) {
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

    fun setupTimer(myComment: String, isAutoTracking: Boolean, myType: String, isManualMode: Boolean,
                   isScheduled: Boolean, timeToSchedule: Long, inactivityTime: Long ){
        comment = myComment
        isAutoTrackingEnable = isAutoTracking
        type = myType
        isManualTrackingEnable = isManualMode
        if (isScheduled){
            scheduledPeriod = timeToSchedule
        }
        inactivityPeriodInMills = inactivityTime
    }

    fun getRecordedTimeInMills() = timeInMills

}


