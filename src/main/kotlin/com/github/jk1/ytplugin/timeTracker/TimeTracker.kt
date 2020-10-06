package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.NoActiveYouTrackTaskException
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.lang.IllegalStateException
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
    var isScheduledEnabled = true
    var isWhenProjectClosedEnabled = true
    var isPostAfterCommitEnabled = true
    var isAutoTrackingEnable = true
    var isRunning = false
    var isPaused = false
    var isAutoTrackingTemporaryDisabled = false
    var activityTracker: ActivityTracker? = null
    var isPostedScheduled = true


    init {
        try {
            val repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
            timeTrackerStoreComponent[repo].update(repo)
            val storedTimer = timeTrackerStoreComponent[repo].getTrackerJson()
            setupTimerFromStored(storedTimer)
            StartTrackerAction().startAutomatedTracking(project, this)
        } catch (e: NoYouTrackRepositoryException) {
            logger.debug("Loading time tracker... Active YouTrack repository is not found: ${e.message}")
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
            isScheduledEnabled = json.asJsonObject.get("isScheduledEnabled").asBoolean
            isWhenProjectClosedEnabled = json.asJsonObject.get("isWhenProjectClosedEnabled").asBoolean
            isPostAfterCommitEnabled = json.asJsonObject.get("isPostAfterCommitEnabled").asBoolean
            isAutoTrackingEnable = json.asJsonObject.get("isAutoTrackingEnable").asBoolean
        }
    }

    fun saveState(repository: YouTrackServer) {
        if (!isPaused) {
            timeInMills += (System.currentTimeMillis() - startTime)
            isPaused = true
        }
        recordedTime = formatTimePeriod(timeInMills)
        timeTrackerStoreComponent[repository].storeTime(repository)

    }

    fun stop() {
        if (isRunning) {
            if (!isPaused) {
                timeInMills += (System.currentTimeMillis() - startTime)
            }
            recordedTime = formatTimePeriod(timeInMills)
            timeInMills = 0
            isRunning = false
            isPaused = false
        } else {
            throw IllegalStateException()
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
        isPostAfterCommitEnabled = isPostWhenCommitEnabled
        isAutoTrackingEnable = isAutoTracking
        isAutoTrackingTemporaryDisabled = false
        isWhenProjectClosedEnabled = isPostOnClosed
        type = myType
        isManualTrackingEnable = isManualMode
        if (isScheduled){
            scheduledPeriod = timeToSchedule
        }
        inactivityPeriodInMills = inactivityTime

        try {
            timeTrackerStoreComponent[repository].update(repository)
        } catch (e: NoActiveYouTrackTaskException){
            logger.debug("Unable to update repository when setting up the timer: ${e.message}")
        }
    }

    fun getRecordedTimeInMills() = timeInMills

}
