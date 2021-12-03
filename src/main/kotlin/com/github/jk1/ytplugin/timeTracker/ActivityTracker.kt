package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.NoActiveYouTrackTaskException
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.github.jk1.ytplugin.timeTracker.actions.StopTrackerAction
import com.intellij.ide.IdeEventQueue
import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ex.WindowManagerEx
import java.awt.AWTEvent
import java.awt.Component
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.lang.System.currentTimeMillis
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JFrame

class ActivityTracker(
        private val parentDisposable: Disposable,
        private val timer: TimeTracker,
        private val inactivityPeriod: Long,
        override val project: Project
) : Disposable, ComponentAware {

    private var trackingDisposable: Disposable? = null
    private var startInactivityTime: Long = currentTimeMillis()
    private var isPostedOnClose = false

    fun startTracking() {
        if (trackingDisposable != null) {
            val trackerNote = TrackerNotification()
            trackerNote.notify("Could not start autonomous tracking at the moment", NotificationType.WARNING)
            logger.debug("Activity tracker is unable to start tracking at the moment: trackingDisposable is not null")
            return
        }
        trackingDisposable = newDisposable(parentDisposable)
        startIDEListener(trackingDisposable!!)
        scheduleListener(trackingDisposable!!)
    }


    companion object {
        fun newDisposable(vararg parents: Disposable, callback: () -> Any = {}): Disposable {
            val isDisposed = AtomicBoolean(false)
            val disposable = Disposable {
                if (!isDisposed.get()) {
                    isDisposed.set(true)
                    callback()
                }
            }
            parents.forEach { parent ->
                // can't use here "Disposer.register(parent, disposable)"
                // because Disposer only allows one parent to one child registration of Disposable objects
                Disposer.register(parent, Disposable { Disposer.dispose(disposable) })
            }
            return disposable
        }
    }

    override fun dispose() {
        if (trackingDisposable != null) {
            Disposer.dispose(trackingDisposable!!)
            trackingDisposable = null
        }
    }

    private fun scheduleListener(parentDisposable: Disposable) {
        IdeEventQueue.getInstance().addPostprocessor(IdeEventQueue.EventDispatcher {

            val currentTime = LocalDateTime.now()
            val formatter = SimpleDateFormat("mm")
            val hour = formatter.format(SimpleDateFormat("mm").parse(currentTime.hour.toString()))
            val minute = formatter.format(SimpleDateFormat("mm").parse(currentTime.minute.toString()))
            val time = hour + ":" + minute + ":" + currentTime.second.toString()

            if (timer.isScheduledEnabled && time == timer.scheduledPeriod) {
                if (!timer.isPostedScheduled) {
                    val trackerNote = TrackerNotification()
                    trackerNote.notify("Scheduled time posting at ${time}0", NotificationType.INFORMATION)
                    timer.isPostedScheduled = true
                    StopTrackerAction().stopTimer(project)
                }
            } else {
                timer.isPostedScheduled = false
            }
            logger.trace("scheduleListener: stop the propagation of an event to other listeners")
            false
        }, parentDisposable)
    }

    private fun startIDEListener(parentDisposable: Disposable) {
        var lastMouseMoveTimestamp = 0L
        val mouseMoveEventsThresholdMs = 1000
        IdeEventQueue.getInstance().addPostprocessor(IdeEventQueue.EventDispatcher { awtEvent: AWTEvent ->

            val store: PropertiesComponent = PropertiesComponent.getInstance(project)
            isPostedOnClose = false
            if (timer.isPaused) {
                timer.pausedTime = currentTimeMillis() - timer.startTime - timer.timeInMills
            }
            // instant caching
            store.saveFields(timer)

            var isMouseOrKeyboardActive = false
            if (awtEvent is MouseEvent && awtEvent.id == MouseEvent.MOUSE_CLICKED) {
                isMouseOrKeyboardActive = captureIdeState()
                logger.trace("state MOUSE_CLICKED $isMouseOrKeyboardActive")
            }
            if (awtEvent is MouseEvent && awtEvent.id == MouseEvent.MOUSE_MOVED) {
                val now = currentTimeMillis()
                if (now - lastMouseMoveTimestamp > mouseMoveEventsThresholdMs) {
                    isMouseOrKeyboardActive = captureIdeState()
                    lastMouseMoveTimestamp = now
                    logger.trace("state MOUSE_MOVED$isMouseOrKeyboardActive")
                }
            }
            if (awtEvent is MouseWheelEvent && awtEvent.id == MouseEvent.MOUSE_WHEEL) {
                val now = currentTimeMillis()
                if (now - lastMouseMoveTimestamp > mouseMoveEventsThresholdMs) {
                    isMouseOrKeyboardActive = captureIdeState()
                    lastMouseMoveTimestamp = now
                    logger.trace("state MOUSE_WHEEL $isMouseOrKeyboardActive")
                }
            }
            if (awtEvent is KeyEvent && awtEvent.id == KeyEvent.KEY_PRESSED) {
                isMouseOrKeyboardActive = captureIdeState()
                logger.trace("state KEYBOARD $isMouseOrKeyboardActive")
            }

            if (!isMouseOrKeyboardActive) {
                if (currentTimeMillis() - startInactivityTime > inactivityPeriod && timer.isRunning &&
                    !timer.isPaused && timer.isAutoTrackingEnable) {
                    timer.pausedTime += (currentTimeMillis() - startInactivityTime - timer.inactivityPeriodInMills)
                    timer.pause("Work timer paused due to inactivity")
                }
            } else if (isMouseOrKeyboardActive) {
                startInactivityTime = currentTimeMillis()

                try {
                    taskManagerComponent.getActiveYouTrackTask()
                    if (!timer.isRunning || timer.isPaused) {
                        StartTrackerAction().startAutomatedTracking(project, timer)
                    }
                } catch (e: NoActiveYouTrackTaskException) {
                    logger.debug("Active task is not valid")
                }
            }
            logger.trace("startIDEListener: stop the propagation of an event to other listeners")
            false
        }, parentDisposable)
    }

    private fun captureIdeState(): Boolean {
        try {
            val ideFocusManager = IdeFocusManager.getGlobalInstance()
            val focusOwner = ideFocusManager.focusOwner

            // this might also work: ApplicationManager.application.isActive(), ApplicationActivationListener
            val ideHasFocus = WindowManagerEx.getInstanceEx().mostRecentFocusedWindow?.isActive
                    ?: findParentFrame(focusOwner)?.isActive
                    ?: false
            return ideHasFocus && currentEditorIn(project) != null
        } catch (e: Exception) {
            logger.debug("IDE state exception: ${e.message}")
        }
        return false
    }

    private fun findParentFrame(component: Component?): JFrame? =
            when (component) {
                null -> null
                is JFrame -> component
                else -> findParentFrame(component.parent)
            }

    private fun currentEditorIn(project: Project): Editor? =
            (FileEditorManagerEx.getInstance(project) as FileEditorManagerEx).selectedTextEditor

}