package com.github.jk1.ytplugin.workflowsDebugConfiguration

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.rest.WorkflowsRestClient
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.VfsTestUtil

class WorkflowRulesHandler {

    fun loadWorkflowRules(workflowName: String, project: Project) {
        val repositories = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories()
        val repo = if (repositories.isNotEmpty()) {
            repositories[0]
        } else null

        ApplicationManager.getApplication().executeOnPooledThread {
            val workflow = if (workflowName.isNotEmpty())
                WorkflowsRestClient(repo!!).getWorkflowWithRules(workflowName)
            else null
            val trackerNote = TrackerNotification()

            if (workflow != null) {
                for (rule in workflow.rules) {
                    WorkflowsRestClient(repo!!).getWorkFlowContent(workflow, rule)
                    val vFile: VirtualFile? = createFile("src/${rule.name}.js", rule.content, project)
                }
                trackerNote.notify("Successfully loaded workflow \"$workflowName\"", NotificationType.INFORMATION)
            } else {
                trackerNote.notify("Workflow \"$workflowName\" is not found", NotificationType.WARNING)
            }
        }
    }

    private fun createFile(path: String, text: String?, project: Project): VirtualFile? {
        return VfsTestUtil.createFile(PlatformTestUtil.getOrCreateProjectBaseDir(project), path, text)
    }
}