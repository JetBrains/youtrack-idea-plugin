package com.github.jk1.ytplugin.workItems

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.timeTracker.TimerWidget
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.TestWindowManager
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.jvm.internal.Intrinsics
import kotlin.test.assertNotEquals


class InputCredentialsTest :  IdeaProjectTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture
    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        val windowManager: WindowManager = TestWindowManager()
        val statusBar = windowManager.getStatusBar(project)
        val myTimer = timeTrackerComponent

        myTimer.isRunning = true
        myTimer.isPaused = false
        if (statusBar?.getWidget("Time Tracking Clock") == null) {
            statusBar?.addWidget(TimerWidget(myTimer, project), project)
        }
    }

    @Test
    fun testTimeChanges() {
        val windowManager: WindowManager = TestWindowManager()
        val statusBar = windowManager.getStatusBar(project)
        val widget: TimerWidget? = statusBar.getWidget("Time Tracking Clock") as TimerWidget?
        if (widget == null) {
            Intrinsics.throwNpe()
        } else {
            val oldTime: String = widget.time()
            TimeUnit.MINUTES.sleep(1L)
            val currentTime: String = widget.time()
            assertNotEquals(oldTime, currentTime, "Time is constant")
        }
    }

    @After
    fun tearDown() {
        fixture.tearDown()
    }
}