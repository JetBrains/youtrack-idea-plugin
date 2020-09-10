package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.github.jk1.ytplugin.timeTracker.actions.StopTrackerAction
import com.intellij.ide.IdeEventQueue
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ex.WindowManagerEx
import com.intellij.openapi.wm.impl.IdeFrameImpl
import java.awt.AWTEvent
import java.awt.Component
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.lang.System.currentTimeMillis
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean


class ActivityTracker(

        private val parentDisposable: Disposable,
        private val timer: TimeTracker,
        private val inactivityPeriod: Long,
        private val project: Project

) : Disposable {
    private var trackingDisposable: Disposable? = null
    private var myInactivityTime: Long = 0
    var startInactivityTime: Long = currentTimeMillis()

    private val repo = project.let { ComponentAware.of(it).taskManagerComponent.getActiveYouTrackRepository() }
    private var isPostedOnClose = false

    fun startTracking() {
        if (trackingDisposable != null) {
            return
        }
        trackingDisposable = newDisposable(parentDisposable)
        startIDEListener(trackingDisposable!!, 1000)
        scheduleListener(trackingDisposable!!)
    }


    override fun dispose() {
        if (trackingDisposable != null) {
            Disposer.dispose(trackingDisposable!!)
            trackingDisposable = null
        }
    }

    private fun newDisposable(vararg parents: Disposable, callback: () -> Any = {}): Disposable {
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
            Disposer.register(parent, Disposable {
                Disposer.dispose(disposable)
            })
        }
        return disposable
    }

    private fun scheduleListener(parentDisposable: Disposable) {
        IdeEventQueue.getInstance().addPostprocessor(IdeEventQueue.EventDispatcher { awtEvent: AWTEvent ->

            val formatter = SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss z")
            val date = Date(currentTimeMillis())
            // select time only
            val time = formatter.format(date).substring(formatter.format(date).length - 12, formatter.format(date).length - 4)
            if (timer.isScheduledUnabled && (time == timer.scheduledPeriod)){
                val trackerNote = TrackerNotification()
                trackerNote.notify("Scheduled time posting", NotificationType.INFORMATION)
                StopTrackerAction().stopTimer(project)
                Thread.sleep(1000)
            }

            false
        }, parentDisposable)
    }


    private fun startIDEListener(parentDisposable: Disposable, mouseMoveEventsThresholdMs: Long) {
        var lastMouseMoveTimestamp = 0L
        IdeEventQueue.getInstance().addPostprocessor(IdeEventQueue.EventDispatcher { awtEvent: AWTEvent ->

            var isMouseOrKeyboardActive = false
            if (awtEvent is MouseEvent && awtEvent.id == MouseEvent.MOUSE_CLICKED) {
                isMouseOrKeyboardActive = captureIdeState()
                logger.debug("state MOUSE_CLICKED " + isMouseOrKeyboardActive)
            }
            if (awtEvent is MouseEvent && awtEvent.id == MouseEvent.MOUSE_MOVED) {
                val now = currentTimeMillis()
                if (now - lastMouseMoveTimestamp > mouseMoveEventsThresholdMs) {
                    isMouseOrKeyboardActive = captureIdeState()
                    lastMouseMoveTimestamp = now
                    logger.debug("state MOUSE_MOVED" + isMouseOrKeyboardActive)

                }
            }
            if (awtEvent is MouseWheelEvent && awtEvent.id == MouseEvent.MOUSE_WHEEL) {
                val now = currentTimeMillis()
                if (now - lastMouseMoveTimestamp > mouseMoveEventsThresholdMs) {
                    isMouseOrKeyboardActive = captureIdeState()
                    lastMouseMoveTimestamp = now
                    logger.debug("state MOUSE_WHEEL " + isMouseOrKeyboardActive)
                }
            }
            if (awtEvent is KeyEvent && awtEvent.id == KeyEvent.KEY_PRESSED) {
                isMouseOrKeyboardActive = captureIdeState()
                logger.debug("state keyboard " + isMouseOrKeyboardActive)
            }
            if (!isMouseOrKeyboardActive) {
                myInactivityTime = currentTimeMillis() - startInactivityTime
                if ((myInactivityTime > inactivityPeriod) && timer.isRunning && !timer.isPaused) {
                    timer.pause()
                }
            } else if (isMouseOrKeyboardActive) {
                myInactivityTime = 0
                startInactivityTime = currentTimeMillis()
                if (!timer.isRunning || timer.isPaused) {
                    val action = StartTrackerAction()
                    action.startAutomatedTracking(project, timer)
                }
            }
            false
        }, parentDisposable)
    }

    private fun captureIdeState(): Boolean {
        try {

            val ideFocusManager = IdeFocusManager.getGlobalInstance()
            // use "lastFocusedFrame" to be able to obtain project in cases when some dialog is open (e.g. "override" or "project settings")
            var currentProject = ideFocusManager.lastFocusedFrame?.project

            if (currentProject == null || currentProject.isDefault) {
                if (!isPostedOnClose){
                    timer.stop()
                    TimeTrackerRestClient(repo).postNewWorkItem(timer.issueId,
                            timer.recordedTime, timer.type, timer.comment, (Date().time).toString())
                    isPostedOnClose = true
                }
                return false
            } else {
                isPostedOnClose = false
            }

            val focusOwner = ideFocusManager.focusOwner

            // this might also work: ApplicationManager.application.isActive(), ApplicationActivationListener
            val window = WindowManagerEx.getInstanceEx().mostRecentFocusedWindow

            var ideHasFocus = window?.isActive
            if (!ideHasFocus!!) {
                val ideFrame = findParentComponent<IdeFrameImpl?>(focusOwner) { it is IdeFrameImpl }
                ideHasFocus = ideFrame != null && ideFrame.isActive
            }

            if (!ideHasFocus) {
                return false
            }

            val editor = currentEditorIn(project)
            if (editor != null) {
                return true
            }

            return false

        } catch (e: Exception) {
            logger.debug("IDE state exception: ${e.message}")
        }
        return false
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findParentComponent(component: Component?, matches: (Component) -> Boolean): T? =
            when {
                component == null -> null
                matches(component) -> component as T?
                else -> findParentComponent(component.parent, matches)
            }

    private fun currentEditorIn(project: Project): Editor? =
            (FileEditorManagerEx.getInstance(project) as FileEditorManagerEx).selectedTextEditor

}