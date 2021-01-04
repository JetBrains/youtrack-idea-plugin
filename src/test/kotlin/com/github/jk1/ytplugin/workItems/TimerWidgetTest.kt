package com.github.jk1.ytplugin.workItems

import com.github.jk1.ytplugin.timeTracker.TimeTracker
import com.github.jk1.ytplugin.timeTracker.TimerWidget
import com.intellij.mock.MockProjectEx
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.TestWindowManager
import com.intellij.testFramework.PlatformLiteFixture
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlin.jvm.internal.Intrinsics
import kotlin.test.assertNotEquals

class TimerWidgetTest : PlatformLiteFixture() {

    public override fun setUp() {
        super.setUp()
        initApplication()
        myProject = MockProjectEx(this.testRootDisposable)
        val windowManager: WindowManager = TestWindowManager()
        val statusBar = windowManager.getStatusBar(myProject)
        getApplication().registerService(WindowManager::class.java, windowManager)
        val myTimer = TimeTracker(myProject)
        myTimer.isRunning = true
        myTimer.isPaused = false
        if (statusBar?.getWidget("Time Tracking Clock") == null) {
            statusBar?.addWidget(TimerWidget(myTimer, myProject), myProject)
        }
    }

    @Test
    fun testTimeChanges() {
        val windowManager: WindowManager = TestWindowManager()
        val statusBar = windowManager.getStatusBar(myProject)
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
}