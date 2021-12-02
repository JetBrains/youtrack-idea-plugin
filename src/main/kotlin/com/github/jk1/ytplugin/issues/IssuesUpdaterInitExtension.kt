package com.github.jk1.ytplugin.issues

import com.github.jk1.ytplugin.ComponentAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

class IssuesUpdaterInitExtension: StartupActivity.Background {

    override fun runActivity(project: Project) {
        // force init on project open
        ComponentAware.of(project).issueUpdaterComponent
    }
}