package com.github.jk1.ytplugin.toolWindow

import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.tasks.TaskRepository

abstract class TestConnectionTask internal constructor(title: String?, project: Project?) : Task.Modal(project, title!!, true) {
    var myException: java.lang.Exception? = null
    protected var myConnection: TaskRepository.CancellableConnection? = null
    override fun onCancel() {
        if (myConnection != null) {
            myConnection!!.cancel()
        }
    }
}