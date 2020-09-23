package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.TimeUnit

@Service
class TimeTracker(override val project: Project) : ComponentAware{

    var issueId: String = "Default"
    var issueIdReadable: String = "Default"
    var inactivityPeriodInMills: Long = 600000
    var type: String = "None"
    var scheduledPeriod: String = "19:00:0"
    var recordedTime: String = "0"
    var timeInMills: Long = 0
    var startTime: Long = 0
    var comment: String = "default comment"
    var isManualTrackingEnable = false
    var isScheduledUnabled = true
    var isWhenProjectClosedUnabled = true
    var isPostAfterCommitUnabled = true
    var isAutoTrackingEnable = true
    var isRunning = false
    var isPaused = false
    var isAutoTrackingTemporaryDisabled = false
    var activityTracker: ActivityTracker? = null
    var isPostedScheduled = true


    init {
        val task = ComponentAware.of(project).taskManagerComponent.getTaskManager().activeTask
        if (task.isIssue) {
            val repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
            timeTrackerStoreComponent[repo].update(repo)
            val storedTimer = timeTrackerStoreComponent[repo].getTrackerJson()
            setupTimerFromStored(storedTimer)
            StartTrackerAction().startAutomatedTracking(project, this)
        }

    }


    fun setupTimerFromStored(parameters: String){
        if (parameters != "0") {
            val json: JsonObject = JsonParser.parseString(parameters) as JsonObject
            json.asJsonObject.get("type").asString
            issueId = json.asJsonObject.get("issueId").asString
            issueIdReadable = json.asJsonObject.get("issueIdReadable").asString
            inactivityPeriodInMills = json.asJsonObject.get("inactivityPeriodInMills").asLong
            type = json.asJsonObject.get("type").asString
            scheduledPeriod = json.asJsonObject.get("scheduledPeriod").asString
            recordedTime = json.asJsonObject.get("recordedTime").asString
            timeInMills = json.asJsonObject.get("timeInMills").asLong
            startTime = json.asJsonObject.get("startTime").asLong
            comment = json.asJsonObject.get("comment").asString
            isManualTrackingEnable = json.asJsonObject.get("isManualTrackingEnable").asBoolean
            isScheduledUnabled = json.asJsonObject.get("isScheduledUnabled").asBoolean
            isWhenProjectClosedUnabled = json.asJsonObject.get("isWhenProjectClosedUnabled").asBoolean
            isPostAfterCommitUnabled = json.asJsonObject.get("isPostAfterCommitUnabled").asBoolean
            isAutoTrackingEnable = json.asJsonObject.get("isAutoTrackingEnable").asBoolean
        }
    }

    fun saveState() {
        if (!isPaused) {
            timeInMills += (System.currentTimeMillis() - startTime)
        }
        recordedTime = formatTimePeriod(timeInMills)
        val repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
        timeTrackerStoreComponent[repo].update(repo)
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
            trackerNote.notify("Timer already paused for issue $issueIdReadable", NotificationType.WARNING)
        } else {
            if (isRunning) {
                timeInMills += (System.currentTimeMillis() - startTime)
                isPaused = true
                trackerNote.notify("Work timer paused for issue $issueIdReadable", NotificationType.INFORMATION)
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
            trackerNote.notify("Work timer reset for issue $issueIdReadable", NotificationType.INFORMATION)
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
                   isScheduled: Boolean, timeToSchedule: String, inactivityTime: Long, isPostOnClosed: Boolean, repository: YouTrackServer){
        comment = myComment
        isPostAfterCommitUnabled = isPostWhenCommitEnabled
        isAutoTrackingEnable = isAutoTracking
        isAutoTrackingTemporaryDisabled = false
        isWhenProjectClosedUnabled = isPostOnClosed
        type = myType
        isManualTrackingEnable = isManualMode
        if (isScheduled){
            scheduledPeriod = timeToSchedule
        }
        inactivityPeriodInMills = inactivityTime

        val task = ComponentAware.of(project).taskManagerComponent.getTaskManager().activeTask

        if (task.isIssue) {
            timeTrackerStoreComponent[repository].update(repository)
        }
    }

    fun getRecordedTimeInMills() = timeInMills

}
