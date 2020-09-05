
package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.actions.PauseTrackerAction
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.intellij.concurrency.JobScheduler
import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.impl.EditorComponentImpl
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.WindowManagerEx
import com.intellij.openapi.wm.impl.IdeFrameImpl
import com.intellij.tasks.TaskManager
import java.awt.AWTEvent
import java.awt.Component
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.lang.System.currentTimeMillis
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JDialog


enum class Type {
    IdeState,
    KeyEvent,
    MouseEvent,
}

class ActivityTracker(

        private val parentDisposable: Disposable,
        private val logTrackerCallDuration: Boolean = false,
        private val timer: TimeTracker,
        private val inactivityPeriod: Long,
        private val repo: YouTrackServer,
        private val project: Project,
        private val taskManager: TaskManager

) {
    private var trackingDisposable: Disposable? = null
    private val trackerCallDurations: MutableList<Long> = mutableListOf()
    private var myInactivityTime: Long = 0
    var startInactivityTime: Long = currentTimeMillis()


    fun startTracking() {
        if (trackingDisposable != null) {
            return
        }
        trackingDisposable = newDisposable(parentDisposable)
//        startPollingIdeState(trackingDisposable!!)
        startAWTEventListener(trackingDisposable!!, true, true, 1000)
    }


    fun stopTracking() {
        if (trackingDisposable != null) {
            Disposer.dispose(trackingDisposable!!)
            trackingDisposable = null
        }
    }

    private fun <T> invokeOnEDT(callback: () -> T): T? {
        var result: T? = null
        ApplicationManager.getApplication()
                .invokeAndWait({ result = callback() }, ModalityState.any())
        return result
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

    private fun Disposable.whenDisposed(callback: () -> Any) = newDisposable(this, callback = callback)

    private fun startPollingIdeState(trackingDisposable: Disposable) {
        val runnable = Runnable {
            // It has to be invokeOnEDT() method so that it's still triggered when IDE dialog window is opened (e.g. project settings).
            invokeOnEDT {
                val isIDEActive = captureIdeState(Type.IdeState, "")
                if (!isIDEActive){
                    myInactivityTime = currentTimeMillis() - startInactivityTime

                    if ((myInactivityTime > inactivityPeriod) && timer.isRunning){
                        timer.pause()
                    }
                }
                else if (isIDEActive ){
                    myInactivityTime = 0
                    if (!timer.isRunning) {
                        startInactivityTime = currentTimeMillis()
                        timer.isRunning = false
                        timer.isPaused = false
                        StartTrackerAction(repo).startAutomatedTracking(project)
                    }
                }
            }
        }

        val nextSecondStartMs = 1000 - (currentTimeMillis() % 1000)
        val future = JobScheduler.getScheduler().scheduleWithFixedDelay(runnable, nextSecondStartMs, 1000, MILLISECONDS)
        trackingDisposable.whenDisposed {
            future.cancel(true)
        }
    }

    private fun trackerCallDurationsEvent(): String? {
        if (!logTrackerCallDuration || trackerCallDurations.size < 10){
            return null
        }

        trackerCallDurations.clear()
        return trackerCallDurations.joinToString(",")
    }

    private fun startAWTEventListener( parentDisposable: Disposable, trackKeyboard: Boolean,
                                       trackMouse: Boolean, mouseMoveEventsThresholdMs: Long
    ) {
        var lastMouseMoveTimestamp = 0L
        IdeEventQueue.getInstance().addPostprocessor(IdeEventQueue.EventDispatcher { awtEvent: AWTEvent ->
            var isMouseOrKeyboardActive = false
            if (trackMouse && awtEvent is MouseEvent && awtEvent.id == MouseEvent.MOUSE_CLICKED) {
                val eventData = "click:" + awtEvent.button + ":" + awtEvent.clickCount + ":" + awtEvent.modifiers
                isMouseOrKeyboardActive = captureIdeState(Type.MouseEvent, eventData)
                logger.debug("state MOUSE_CLICKED " + isMouseOrKeyboardActive)
            }
            if (trackMouse && awtEvent is MouseEvent && awtEvent.id == MouseEvent.MOUSE_MOVED) {
                val now = currentTimeMillis()
                if (now - lastMouseMoveTimestamp > mouseMoveEventsThresholdMs) {
                    isMouseOrKeyboardActive = captureIdeState(Type.MouseEvent, "move:" + awtEvent.x + ":" + awtEvent.y + ":" + awtEvent.modifiers)
                    logger.debug("state MOUSE_MOVED" + isMouseOrKeyboardActive)
                    lastMouseMoveTimestamp = now
                }
            }
            if (trackMouse && awtEvent is MouseWheelEvent && awtEvent.id == MouseEvent.MOUSE_WHEEL) {
                val now = currentTimeMillis()
                if (now - lastMouseMoveTimestamp > mouseMoveEventsThresholdMs) {
                    isMouseOrKeyboardActive = captureIdeState(Type.MouseEvent, "wheel:" + awtEvent.wheelRotation + ":" + awtEvent.modifiers)
                    logger.debug("state MOUSE_WHEEL " + isMouseOrKeyboardActive)
                    lastMouseMoveTimestamp = now
                }
            }
            if (trackKeyboard && awtEvent is KeyEvent && awtEvent.id == KeyEvent.KEY_PRESSED) {
                isMouseOrKeyboardActive = captureIdeState(Type.KeyEvent, "" + (awtEvent.keyChar.toInt()) + ":" + awtEvent.keyCode + ":" + awtEvent.modifiers)
                logger.debug("state keyboard " + isMouseOrKeyboardActive)
            }
            if (!isMouseOrKeyboardActive){
                myInactivityTime = currentTimeMillis() - startInactivityTime
//                println("inactivity time: " + myInactivityTime)

                if((myInactivityTime > inactivityPeriod) && timer.isRunning && !timer.isPaused){
                    timer.pause()
//                    println("timer paused ")

                }
            } else if (isMouseOrKeyboardActive) {
                myInactivityTime = 0
//                println("time: " + myInactivityTime)

                startInactivityTime = currentTimeMillis()
                if (!timer.isRunning || timer.isPaused) {
                    val action = StartTrackerAction(repo)
                    action.startAutomatedTracking(project)
                }
            }
            false
        }, parentDisposable)
    }

    private fun captureIdeState(eventType: Type, originalEventData: String): Boolean {
        val start = currentTimeMillis()
        try {
            var eventData = originalEventData
            if (eventType == Type.IdeState) {
                eventData = "Inactive"
            }

            val ideFocusManager = IdeFocusManager.getGlobalInstance()
            val focusOwner = ideFocusManager.focusOwner

            // this might also work: ApplicationManager.application.isActive(), ApplicationActivationListener
            val window = WindowManagerEx.getInstanceEx().mostRecentFocusedWindow

            var ideHasFocus = window?.isActive
            if (!ideHasFocus!!) {
                @Suppress("UnstableApiUsage")
                val ideFrame = findParentComponent<IdeFrameImpl?>(focusOwner) { it is IdeFrameImpl }
                ideHasFocus = ideFrame != null && ideFrame.isActive
            }
            if (!ideHasFocus) return false

            // use "lastFocusedFrame" to be able to obtain project in cases when some dialog is open (e.g. "override" or "project settings")
            val project = ideFocusManager.lastFocusedFrame?.project
            if (eventType == Type.IdeState && project?.isDefault != false) {
                eventData = "NoProject"
            }
            if (project == null || project.isDefault) return false

            if (eventType == Type.IdeState) {
                eventData = "Active"
            }

            // Check for JDialog before EditorComponentImpl because dialog can belong to editor.
            val focusOwnerId = when {
                findParentComponent<JDialog>(focusOwner) { it is JDialog } != null                         -> "Dialog"
                findParentComponent<EditorComponentImpl>(focusOwner) { it is EditorComponentImpl } != null -> "Editor"
                else                                                                                       -> {
                    val toolWindowId = ToolWindowManager.getInstance(project).activeToolWindowId
                    toolWindowId ?: "Popup"
                }
            }
            val editor = currentEditorIn(project)
            if (editor != null) {
                return true
            }

            return false

        } catch (e: Exception) {
            return false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> findParentComponent(component: Component?, matches: (Component) -> Boolean): T? =
            when {
                component == null  -> null
                matches(component) -> component as T?
                else               -> findParentComponent(component.parent, matches)
            }

    private fun currentEditorIn(project: Project): Editor? =
            (FileEditorManagerEx.getInstance(project) as FileEditorManagerEx).selectedTextEditor

}