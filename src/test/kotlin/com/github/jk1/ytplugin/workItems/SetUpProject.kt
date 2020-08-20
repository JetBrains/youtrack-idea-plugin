package com.github.jk1.ytplugin.workItems

import com.github.jk1.ytplugin.timeTracker.ClockWidget
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.WindowManager
import kotlin.jvm.internal.Intrinsics

class SetUpProject(project: Project) {
    private val project: Project
    fun projectOpened() {
        val bar = WindowManager.getInstance().getStatusBar(project)
        bar?.addWidget(ClockWidget(System.currentTimeMillis()))
    }

    init {
        Intrinsics.checkParameterIsNotNull(project, "project")
        this.project = project
    }
}