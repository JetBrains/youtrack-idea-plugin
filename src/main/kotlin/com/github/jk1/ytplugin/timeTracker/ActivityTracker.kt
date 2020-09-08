package com.github.jk1.ytplugin.timeTracker

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.timeTracker.actions.StartTrackerAction
import com.intellij.concurrency.JobScheduler
import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
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
import java.util.*
import java.util.concurrent.TimeUnit.MILLISECONDS
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


    fun startTracking() {
        if (trackingDisposable != null) {
            return
        }
        trackingDisposable = newDisposable(parentDisposable)
        startPollingProjectState()
        startIDEListener(trackingDisposable!!, 1000)
    }


    private fun <T> invoke(callback: () -> T): T? {
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

    private fun startPollingProjectState() {
        val runnable = Runnable {
            invoke {
                val isProjectActive = captureIdeState()
                if (!isProjectActive) {
                    logger.debug("state PROJECT_INACTIVE")
                }
            }
        }

        val nextSecondStartMs = 1000 - (currentTimeMillis() % 1000)
        val future = JobScheduler.getScheduler().scheduleWithFixedDelay(runnable, nextSecondStartMs, 1000, MILLISECONDS)
        trackingDisposable?.whenDisposed {
            future.cancel(true)
        }
    }

    private fun startIDEListener(parentDisposable: Disposable, mouseMoveEventsThresholdMs: Long) {
        var lastMouseMoveTimestamp = 0L

        IdeEventQueue.getInstance().addPostprocessor(IdeEventQueue.EventDispatcher { awtEvent: AWTEvent ->
            var isMouseOrKeyboardActive = false

            if (awtEvent is MouseEvent && awtEvent.id == MouseEvent.MOUSE_CLICKED) {
                isMouseOrKeyboardActive = captureIdeState()
                logger.debug("state MOUSE_CLICKED $isMouseOrKeyboardActive")
            }

            if (awtEvent is MouseEvent && awtEvent.id == MouseEvent.MOUSE_MOVED) {
                val now = currentTimeMillis()
                if (now - lastMouseMoveTimestamp > mouseMoveEventsThresholdMs) {
                    isMouseOrKeyboardActive = captureIdeState()
                    lastMouseMoveTimestamp = now
                    logger.debug("state MOUSE_MOVED $isMouseOrKeyboardActive")
                }
            }

            if (awtEvent is MouseWheelEvent && awtEvent.id == MouseEvent.MOUSE_WHEEL) {
                val now = currentTimeMillis()
                if (now - lastMouseMoveTimestamp > mouseMoveEventsThresholdMs) {
                    isMouseOrKeyboardActive = captureIdeState()
                    lastMouseMoveTimestamp = now
                    logger.debug("state MOUSE_WHEEL $isMouseOrKeyboardActive")
                }
            }

            if (awtEvent is KeyEvent && awtEvent.id == KeyEvent.KEY_PRESSED) {
                isMouseOrKeyboardActive = captureIdeState()
                logger.debug("state keyboard $isMouseOrKeyboardActive")
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
                    action.startAutomatedTracking(project)
                }
            }
            false
        }, parentDisposable)
    }

    private fun captureIdeState(): Boolean {
        try {
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

            if (!ideHasFocus) {
                return false
            }

            // use "lastFocusedFrame" to be able to obtain project in cases when some dialog is open (e.g. "override" or "project settings")
            val project = ideFocusManager.lastFocusedFrame?.project

            if (project == null || project.isDefault) {
                if (project != null) {
                    val repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
                    TimeTrackerRestClient(repo).postNewWorkItem(timer.issueId,
                            timer.recordedTime, timer.type, timer.comment, (Date().time).toString())
                }
                return false
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

    override fun dispose() {
        if (trackingDisposable != null) {
            Disposer.dispose(trackingDisposable!!)
            trackingDisposable = null
        }
    }

}