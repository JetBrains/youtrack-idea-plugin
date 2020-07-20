package com.github.jk1.ytplugin.setupWindow.Connection

import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

abstract class TestConnectionTask internal constructor(title: String?, project: Project?) : Task.Modal(project, title!!, true) {
    var myException: java.lang.Exception? = null
    protected var myConnection: CancellableConnection? = null
    override fun onCancel() {
        if (myConnection != null) {
            myConnection!!.cancel()
        }
    }
}