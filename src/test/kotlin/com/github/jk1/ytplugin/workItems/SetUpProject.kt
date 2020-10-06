package com.github.jk1.ytplugin.workItems

import com.intellij.openapi.project.Project
import kotlin.jvm.internal.Intrinsics

class SetUpProject(project: Project) {
    private val project: Project
    fun projectOpened() {
    }

    init {
        Intrinsics.checkParameterIsNotNull(project, "project")
        this.project = project
    }
}