package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.TimeUnit

@Service
class TimeTracker(override val project: Project) : ComponentAware{

    var issueId: String = "Default"
    var inactivityPeriodInMills: Long = 600000
    var type: String = "None"
    var scheduledPeriod: String = "19:00"
    var recordedTime: String = ""
    var timeInMills: Long = 0
    var startTime: Long = 0
    var comment: String = "default comment"
    var isManualTrackingEnable = true
    var isScheduledUnabled = true
    var isWhenProjectClosedUnabled = true
    var isPostAfterCommitUnabled = true
    var isAutoTrackingEnable = true
    var isRunning = false
    var isPaused = false
    var isAutoTrackingTemporaryDisabled = false
    var activityTracker: ActivityTracker? = null

    init {
        if (isAutoTrackingEnable){
            val repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
            timeTrackerStoreComponent[repo].update(repo)
            val storedTimer = timeTrackerStoreComponent[repo].getTracker()
            val listOfParameters = parseTimerParameters(storedTimer)
            setupTimerFromStored(listOfParameters)

            StartTrackerAction().startAutomatedTracking(project, this)
        }
    }


    fun parseTimerParameters(timerString: String) : List<String>{
        return timerString.split(" ")
    }

//    tracker = timer.isAutoTrackingEnable.toString() + " " + timer.isManualTrackingEnable.toString() + " " +
//    timer.inactivityPeriodInMills + " " + timer.comment + " " +
//    timer.isScheduledUnabled.toString() + " " + timer.scheduledPeriod + " " +
//    timer.type + " " + timer.timeInMills + " " + timer.isRunning.toString()

    fun setupTimerFromStored(parameters: List<String>){
        if (parameters.size == 9){
            inactivityPeriodInMills  = parameters[2].toLong()
            type = parameters[6]
            if (parameters[4] == "false"){
                isScheduledUnabled = false
            } else {
                scheduledPeriod = parameters[5]
            }
            comment = parameters[3]
            timeInMills = parameters[7].toLong()
            isPaused = true

            if (parameters[0] == "false"){
                isAutoTrackingEnable = false
            }
            if (parameters[1] == "false"){
                isManualTrackingEnable = false
            }
        }
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
            trackerNote.notify("Could not stop time tracking: timer is not started", NotificationType.WARNING)
            "0"
        }
    }

    fun pause() {
        val trackerNote = TrackerNotification()
        if (isPaused) {
            trackerNote.notify("Timer already paused", NotificationType.WARNING)
        } else {
            if (isRunning) {
                timeInMills += (System.currentTimeMillis() - startTime)
                isPaused = true
                trackerNote.notify("Work timer paused", NotificationType.INFORMATION)
            } else {
                trackerNote.notify("Could not pause - timer is not started", NotificationType.WARNING)
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


    fun reset() {
        if (isRunning) {
            isPaused = false
            recordedTime = "0"
            timeInMills = 0
            startTime = System.currentTimeMillis()
            val trackerNote = TrackerNotification()
            trackerNote.notify("Work timer reset", NotificationType.INFORMATION)
        } else {
            val trackerNote = TrackerNotification()
            trackerNote.notify("Could not reset - timer is not started", NotificationType.WARNING)
        }
    }

    private fun formatTimePeriod(timeInMilSec: Long): String {
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMilSec)
        return if (minutes > 0)
            minutes.toString()
        else
            "0"
    }

    fun setupTimer(myComment: String, isPostWhenCommitEnabled: Boolean, isAutoTracking: Boolean, myType: String, isManualMode: Boolean,
                   isScheduled: Boolean, timeToSchedule: String, inactivityTime: Long ){
        comment = myComment
        isPostAfterCommitUnabled = isPostWhenCommitEnabled
        isAutoTrackingEnable = isAutoTracking
        isAutoTrackingTemporaryDisabled = false
        type = myType
        isManualTrackingEnable = isManualMode
        if (isScheduled){
            scheduledPeriod = timeToSchedule
        }
        inactivityPeriodInMills = inactivityTime

        val repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
        timeTrackerStoreComponent[repo].update(repo)

    }

    fun getRecordedTimeInMills() = timeInMills

}


