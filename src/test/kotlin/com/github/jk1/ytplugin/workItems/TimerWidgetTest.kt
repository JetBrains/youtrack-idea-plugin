package com.github.jk1.ytplugin.workItems

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.timeTracker.TimerWidget
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.impl.TestWindowManager
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class InputCredentialsTest :  IdeaProjectTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture
    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        val statusBar = TestWindowManager().getStatusBar(project)
        val timer = timeTrackerComponent
        timer.reset()
        timer.isRunning = true
        timer.isPaused = false
        if (statusBar.getWidget("Time Tracking Clock") == null) {
            statusBar.addWidget(TimerWidget(timer, project, project), project)
        }
    }

    @Test
    fun  `test time changes with previously saved time`() {
        val statusBar = TestWindowManager().getStatusBar(project)
        val widget = statusBar.getWidget("Time Tracking Clock") as TimerWidget

        // saved 2 min for active task
        spentTimePerTaskStorage.setSavedTimeForLocalTask(taskManagerComponent.getActiveTask().id, 120000)

        TimeUnit.MINUTES.sleep(1L)
        val currentTime = widget.time()

        Assert.assertEquals(currentTime, "Time spent on issue Default: 00h 03m")
    }


    @Test
    fun `test time changes without previously saved time`() {
        val statusBar = TestWindowManager().getStatusBar(project)
        val widget = statusBar.getWidget("Time Tracking Clock") as TimerWidget
        val oldTime = widget.time()
        TimeUnit.MINUTES.sleep(1L)
        val currentTime = widget.time()
        Assert.assertNotEquals(oldTime, currentTime, "Time is constant")
    }

    @After
    fun tearDown() {
        fixture.tearDown()
    }
}