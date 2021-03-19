package com.github.jk1.ytplugin.workflowsDebugConfiguration

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.rest.WorkflowsRestClient
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
                WorkflowsRestClient(repo!!).getWorkflowRulesList(workflowName)
            else null

            if (workflow != null) {
                for (rule in workflow.rules) {
                    WorkflowsRestClient(repo!!).getWorkFlowContent(workflow, rule)
                    val vFile: VirtualFile? = createFile("src/${rule.name}.js", rule.content, project)
                }
            }
        }
    }

    private fun createFile(path: String, text: String?, project: Project): VirtualFile? {
        return VfsTestUtil.createFile(PlatformTestUtil.getOrCreateProjectBaseDir(project), path, text)
    }
}