package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.NoActiveYouTrackTaskException
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.PropertyName
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.TimeUnit

@Service
class TimeTracker(override val project: Project) : ComponentAware{

    @PropertyName("timeTracker.issueId")
    var issueId: String = "Default"

    @PropertyName("timeTracker.issueIdReadable")
    var issueIdReadable: String = "Default"

    @PropertyName("timeTracker.inactivityPeriodInMills")
    var inactivityPeriodInMills: Long = 600000

    @PropertyName("timeTracker.pausedTime")
    var pausedTime: Long = 0

    @PropertyName("timeTracker.type")
    var type: String = "None"

    @PropertyName("timeTracker.scheduledPeriod")
    var scheduledPeriod: String = "19:00:0"

    @PropertyName("timeTracker.recordedTime")
    var recordedTime: String = "0"

    @PropertyName("timeTracker.timeInMills")
    var timeInMills: Long = 0

    @PropertyName("timeTracker.startTime")
    var startTime: Long = 0

    @PropertyName("timeTracker.comment")
    var comment: String = "default comment"

    @PropertyName("timeTracker.isManualTrackingEnable")
    var isManualTrackingEnable = false

    @PropertyName("timeTracker.isScheduledEnabled")
    var isScheduledEnabled = true

    @PropertyName("timeTracker.isWhenProjectClosedEnabled")
    var isWhenProjectClosedEnabled = true

    @PropertyName("timeTracker.isPostAfterCommitEnabled")
    var isPostAfterCommitEnabled = true

    @PropertyName("timeTracker.isAutoTrackingEnable")
    var isAutoTrackingEnable = true

    @PropertyName("timeTracker.isRunning")
    var isRunning = false

    @PropertyName("timeTracker.isPaused")
    var isPaused = false

    @PropertyName("timeTracker.isAutoTrackingTemporaryDisabled")
    var isAutoTrackingTemporaryDisabled = false

    @PropertyName("timeTracker.sactivityTracker")
    var activityTracker: ActivityTracker? = null

    @PropertyName("timeTracker.isPostedScheduled")
    var isPostedScheduled = true


    init {
        try {
            val repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
            timeTrackerStoreComponent[repo].update(repo)
            val store: PropertiesComponent = PropertiesComponent.getInstance(project)
            store.loadFields(this)
            println("hey2: " + timeInMills + " " + isWhenProjectClosedEnabled + " " + startTime + isPaused)

            StartTrackerAction().startAutomatedTracking(project, this)
        } catch (e: NoYouTrackRepositoryException) {
            logger.debug("Loading time tracker... Active YouTrack repository is not found: ${e.message}")
        }
    }


    fun saveState() {
        if (!isPaused) {
            timeInMills += (System.currentTimeMillis() - startTime)
            isPaused = true
        }
        recordedTime = formatTimePeriod(timeInMills)
        val store: PropertiesComponent = PropertiesComponent.getInstance(project)
        store.saveFields(this)
        this.activityTracker?.let { store.saveFields(it) }
        println("save: " + store.getValue("timeTracker.timeInMills") + ' ' +
                store.getValue("timeTracker.timeInMills") + ' ' + store.getValue("activityTracker.startInactivityTime")  )
    }

    fun stop() {
        if (isRunning) {
//            if (!isPaused) {
//                timeInMills += (System.currentTimeMillis() - startTime)
//            }
            recordedTime = formatTimePeriod(timeInMills)
            println("recorded: " + recordedTime + " in mills " + timeInMills)
            timeInMills = 0
            isRunning = false
            isPaused = false
            pausedTime = 0
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
//                timeInMills += (System.currentTimeMillis() - startTime)
                recordedTime = formatTimePeriod(timeInMills)
                isPaused = true
                pausedTime = 0
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

    fun formatTimePeriod(timeInMilSec: Long): String {
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
