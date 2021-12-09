package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.NoActiveYouTrackTaskException
import com.github.jk1.ytplugin.tasks.NoYouTrackRepositoryException
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.PropertyName
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import java.util.concurrent.TimeUnit

@Service
class TimeTracker(override val project: Project) : ComponentAware {

    @PropertyName("timeTracker.issueId")
    var issueId: String = "Default"

    @PropertyName("timeTracker.issueIdReadable")
    var issueIdReadable: String = "Default"

    @PropertyName("timeTracker.inactivityPeriodInMills")
    var inactivityPeriodInMills: Long = 600000

    @PropertyName("timeTracker.pausedTime")
    @Volatile
    var pausedTime: Long = 0

    @PropertyName("timeTracker.type")
    var type: String = "None"

    @PropertyName("timeTracker.scheduledPeriod")
    var scheduledPeriod: String = "19:00:0"

    @PropertyName("timeTracker.recordedTime")
    @Volatile
    var recordedTime: String = "0"

    @PropertyName("timeTracker.timeInMills")
    @Volatile
    var timeInMills: Long = 0

    @PropertyName("timeTracker.startTime")
    var startTime: Long = 0

    @PropertyName("timeTracker.comment")
    var comment: String = ""

    @PropertyName("timeTracker.isManualTrackingEnable")
    var isManualTrackingEnabled = false

    @PropertyName("timeTracker.isScheduledEnabled")
    var isScheduledEnabled = true

    @PropertyName("timeTracker.isWhenProjectClosedEnabled")
    var isWhenProjectClosedEnabled = true

    @PropertyName("timeTracker.isPostAfterCommitEnabled")
    var isPostAfterCommitEnabled = true

    @PropertyName("timeTracker.isAutoTrackingEnable")
    var isAutoTrackingEnabled = false

    @PropertyName("timeTracker.isRunning")
    var isRunning = false

    @PropertyName("timeTracker.isPaused")
    var isPaused = false

    @PropertyName("timeTracker.isAutoTrackingTemporaryDisabled")
    var isAutoTrackingTemporaryDisabled = false

    @PropertyName("timeTracker.activityTracker")
    var activityTracker: ActivityTracker? = null

    @PropertyName("timeTracker.isPostedScheduled")
    var isPostedScheduled = true

    @PropertyName("timeTracker.query")
    var searchQuery: String = ""

    companion object {
        fun formatTimePeriod(timeInMilSec: Long): String {
            val minutes = TimeUnit.MILLISECONDS.toMinutes(timeInMilSec)
            return if (minutes > 0)
                minutes.toString()
            else
                "0"
        }
    }

    init {
        try {
            try {
                val store: PropertiesComponent = PropertiesComponent.getInstance(project)
                store.loadFields(this)
                printTimerSettingsLog()
            } catch (e: IllegalStateException) {
                logger.debug("No time tracker is stored yet")
            }
            isPaused = true
            isAutoTrackingTemporaryDisabled = false

            if (isWhenProjectClosedEnabled) {
                reset()
                spentTimePerTaskStorage.removeAllSavedItems()
            }

            if (isManualTrackingEnabled) {
                reset()
                stop()
            }

            if (isAutoTrackingEnabled) {
                try {
                    taskManagerComponent.getActiveYouTrackTask().id
                    StartTrackerAction().startAutomatedTracking(project, this)
                } catch (e: NoActiveYouTrackTaskException) {
                    logger.debug("TaskId could not be received")
                    isPaused = false
                    isAutoTrackingTemporaryDisabled = true
                }
            }

        } catch (e: NoYouTrackRepositoryException) {
            logger.debug("Loading time tracker... Active YouTrack repository is not found: ${e.message}")
        }
    }

    fun stop() {
        if (isRunning) {
            val task = taskManagerComponent.getActiveTask()
            val storedTime = spentTimePerTaskStorage.getSavedTimeForLocalTask(task.id)

            timeInMills = System.currentTimeMillis() - startTime - pausedTime + storedTime
            // to be used for the post request later
            recordedTime = formatTimePeriod(timeInMills)
            startTime = System.currentTimeMillis()
            timeInMills = 0
            pausedTime = 0
            isRunning = false
            isPaused = false
            isAutoTrackingTemporaryDisabled = false

            saveUpdatedFields()
        } else {
            logger.debug("Timer is not running to stop")
        }
    }

    fun pause(message: String) {
        val trackerNote = TrackerNotification()
        if (isPaused) {
            trackerNote.notify("Timer already paused for issue $issueIdReadable", NotificationType.WARNING)
        } else {
            if (isRunning) {
                timeInMills = System.currentTimeMillis() - startTime - pausedTime
                recordedTime = formatTimePeriod(timeInMills)
                isPaused = true
                trackerNote.notify("$message for $issueIdReadable", NotificationType.INFORMATION)
            } else {
                trackerNote.notify("Could not pause - timer is not started", NotificationType.WARNING)
            }
        }
    }

    fun start(idReadable: String) {
        val trackerNote = TrackerNotification()
        trackerNote.notify("Work timer started for $idReadable", NotificationType.INFORMATION)
        isRunning = true
        isPaused = false
    }

    fun reset() {
            isPaused = false
            recordedTime = "0"
            timeInMills = 0
            pausedTime = 0
            startTime = System.currentTimeMillis()
    }


    fun resetTimeOnly() {
        recordedTime = "0"
        timeInMills = 0
    }

    fun updateIdOnTaskSwitching() {
        try {
            val activeTask = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackTask()
            issueId = activeTask.id
            issueIdReadable = activeTask.id
        } catch (e: NoActiveYouTrackTaskException){
            val task = ComponentAware.of(project).taskManagerComponent.getTaskManager().activeTask
            logger.debug("Selected task $task, ${task.id}" +
                    " is not active for current YouTrack repository")
        }
    }

    fun setupTimerProperties(isAutoTracking: Boolean, isManualMode: Boolean, isScheduled: Boolean,
                             timeToSchedule: String, inactivityTime: Long) {
        isAutoTrackingEnabled = isAutoTracking
        isAutoTrackingTemporaryDisabled = false
        isManualTrackingEnabled = isManualMode
        inactivityPeriodInMills = inactivityTime
        if (isScheduled) { scheduledPeriod = timeToSchedule }

        try {
            saveUpdatedFields()
        } catch (e: NoActiveYouTrackTaskException) {
            logger.debug("Unable to update repository when setting up the timer: ${e.message}")
        }
    }

    private fun saveUpdatedFields() {
        val store: PropertiesComponent = PropertiesComponent.getInstance(project)
        store.saveFields(this)
    }

    fun setupValuesNotRequiringTimerStop(type: String, comment: String, postWhenCommitCheckbox: JBCheckBox,
                                                 postWhenProjectClosedCheckbox: JBCheckBox) {
        setWorkItemsType(type)
        setDefaultComment(comment)
        setPostWhenCommitEnabled(postWhenCommitCheckbox.isSelected && postWhenCommitCheckbox.isEnabled)
        setOnProjectCloseEnabled(postWhenProjectClosedCheckbox.isSelected && postWhenProjectClosedCheckbox.isEnabled)
    }

    private fun setWorkItemsType(type: String?) {
        if (type != null && type != this.type) {
            this.type = type
        }
    }

    private fun setDefaultComment(comment: String?) {
        if (comment != null && comment != this.comment) {
            this.comment = comment
        }
    }

    private fun setPostWhenCommitEnabled(isEnabled: Boolean) {
        isPostAfterCommitEnabled = isEnabled
    }

    private fun setOnProjectCloseEnabled(isEnabled: Boolean) {
        isWhenProjectClosedEnabled = isEnabled
    }

    fun getRecordedTimeInMills() = timeInMills

    fun printTimerSettingsLog() {
        logger.debug(
            """
            Time tracking settings: 
            issueId:$issueId
            issueIdReadable$issueIdReadable
            inactivityPeriodInMills $inactivityPeriodInMills
            pausedTime $pausedTime
            scheduledPeriod $scheduledPeriod
            recordedTime $recordedTime
            timeInMills$timeInMills
            startTime $startTime
            isManualTrackingEnable $isManualTrackingEnabled
            isAutoTrackingEnable $isAutoTrackingEnabled
            comment$comment
            isScheduledEnabled $isScheduledEnabled
            isWhenProjectClosedEnabled$isWhenProjectClosedEnabled
            isPostAfterCommitEnabled $isPostAfterCommitEnabled
            isRunning $isRunning
            isPaused $isPaused
            isAutoTrackingTemporaryDisabled $isAutoTrackingTemporaryDisabled
            isPostedScheduled $isPostedScheduled
            searchQuery$searchQuery
            """.trimIndent()
        )
    }

}
