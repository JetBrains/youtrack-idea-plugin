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
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import java.awt.AWTEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.lang.System.currentTimeMillis
import java.util.*
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicBoolean


enum class Type {
    IdeState,
    KeyEvent,
    MouseEvent,
}

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
        startIDEListener(trackingDisposable!!, true, true, 1000)
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
                val isProjectActive = captureProjectState(Type.IdeState)
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

    private fun startIDEListener(parentDisposable: Disposable, trackKeyboard: Boolean,
                                 trackMouse: Boolean, mouseMoveEventsThresholdMs: Long) {
        var lastMouseMoveTimestamp = 0L

        IdeEventQueue.getInstance().addPostprocessor(IdeEventQueue.EventDispatcher { awtEvent: AWTEvent ->
            var isMouseOrKeyboardActive = false

            if (trackMouse && awtEvent is MouseEvent && awtEvent.id == MouseEvent.MOUSE_CLICKED) {
                isMouseOrKeyboardActive = captureProjectState(Type.MouseEvent)
                logger.debug("state MOUSE_CLICKED $isMouseOrKeyboardActive")
            }

            if (trackMouse && awtEvent is MouseEvent && awtEvent.id == MouseEvent.MOUSE_MOVED) {
                val now = currentTimeMillis()
                if (now - lastMouseMoveTimestamp > mouseMoveEventsThresholdMs) {
                    isMouseOrKeyboardActive = captureProjectState(Type.MouseEvent)
                    lastMouseMoveTimestamp = now
                    logger.debug("state MOUSE_MOVED $isMouseOrKeyboardActive")
                }
            }

            if (trackMouse && awtEvent is MouseWheelEvent && awtEvent.id == MouseEvent.MOUSE_WHEEL) {
                val now = currentTimeMillis()
                if (now - lastMouseMoveTimestamp > mouseMoveEventsThresholdMs) {
                    isMouseOrKeyboardActive = captureProjectState(Type.MouseEvent)
                    lastMouseMoveTimestamp = now
                    logger.debug("state MOUSE_WHEEL $isMouseOrKeyboardActive")
                }
            }

            if (trackKeyboard && awtEvent is KeyEvent && awtEvent.id == KeyEvent.KEY_PRESSED) {
                isMouseOrKeyboardActive = captureProjectState(Type.KeyEvent)
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

    private fun captureProjectState(eventType: Type): Boolean {
        try {
            val ideFocusManager = IdeFocusManager.getGlobalInstance()

            // use "lastFocusedFrame" to be able to obtain project in cases when some dialog is open (e.g. "override" or "project settings")
            val project = ideFocusManager.lastFocusedFrame?.project
            if (eventType == Type.IdeState && project?.isDefault != false) {
                if (project != null) {
                    val repo = ComponentAware.of(project).taskManagerComponent.getActiveYouTrackRepository()
                    TimeTrackerRestClient(repo).postNewWorkItem(timer.issueId,
                            timer.recordedTime, timer.type, timer.comment, (Date().time).toString())
                }
                return false
            }
            return true

        } catch (e: Exception) {
            return false
        }
    }

    override fun dispose() {
        if (trackingDisposable != null) {
            Disposer.dispose(trackingDisposable!!)
            trackingDisposable = null
        }
    }

}