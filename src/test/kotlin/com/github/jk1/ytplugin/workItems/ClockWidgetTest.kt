package com.github.jk1.ytplugin.workItems

import com.github.jk1.ytplugin.timeTracker.ClockWidget
import com.intellij.mock.MockProjectEx
import com.intellij.openapi.wm.WindowManager
import com.intellij.openapi.wm.impl.TestWindowManager
import com.intellij.testFramework.PlatformLiteFixture
import java.util.concurrent.TimeUnit
import kotlin.jvm.internal.Intrinsics
import kotlin.test.assertNotEquals

class ClockWidgetTest : PlatformLiteFixture() {
    private var widget: ClockWidget? = null
    @Throws(Exception::class)
    public override fun setUp() {
        super.setUp()
        initApplication()
        myProject = MockProjectEx(this.testRootDisposable)
        val windowManager: WindowManager = TestWindowManager()
        getApplication().registerService(WindowManager::class.java, windowManager)
        val registration = SetUpProject(myProject)
        registration.projectOpened()
        val statusBar = windowManager.getStatusBar(myProject)
        val statusBarWidget = statusBar.getWidget("Time Tracking Clock")
        if (statusBarWidget == null) {
            throw TypeCastException("null cannot be cast to non-null type clock.ClockWidget")
        } else {
            widget = statusBarWidget as ClockWidget?
            val widget: ClockWidget? = widget
            widget?.install(statusBar)
        }
    }

    @Throws(InterruptedException::class)
    fun testTimeChanges() {
        val widget: ClockWidget? = widget
        if (widget == null) {
            Intrinsics.throwNpe()
        }
        else{
            val oldTime: String = widget.time()
            TimeUnit.MINUTES.sleep(1L)
            val currentTime: String = widget.time()
            assertNotEquals(oldTime, currentTime, "Time is constant")
        }
    }
}