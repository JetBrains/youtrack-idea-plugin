package com.github.jk1.ytplugin.workflowsDebugConfiguration

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.WorkflowsRestClient
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.StdFileTypes.JS
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import org.jetbrains.annotations.NotNull
import java.util.concurrent.Callable
import com.intellij.psi.impl.file.PsiDirectoryFactory

import com.intellij.psi.PsiFile
import org.jetbrains.annotations.NonNls


class WorkflowRulesHandler {

    fun loadWorkflowRules(project: Project) {
        val repositories = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories()
        val repo = if (repositories.isNotEmpty()) {
            repositories.first()
        } else null

        ApplicationManager.getApplication().executeOnPooledThread(
                Callable {
                    val workflowsList = WorkflowsRestClient(repo!!).getWorkflowsWithRules()
                    val trackerNote = TrackerNotification()

                    for (workflow in workflowsList) {
                        for (rule in workflow.rules) {
                            WorkflowsRestClient(repo).getWorkFlowContent(workflow, rule)
                            createFile("${rule.name}.js", rule.content, project)
                        }
                        trackerNote.notify("Successfully loaded workflow \"${workflow.name}\"", NotificationType.INFORMATION)
                    }
                })
    }

    private fun createFile(name: String, text: String?, project: Project) {

        ApplicationManager.getApplication().invokeLater {

            val psiFileFactory = PsiFileFactory.getInstance(project)
            val file: PsiFile = psiFileFactory.createFileFromText(name, JS, text as @NotNull @NonNls CharSequence)

            ApplicationManager.getApplication().runWriteAction {
                // find or create directory
                val targetVirtualDir = if (project.baseDir.findFileByRelativePath("src") == null) {
                    logger.debug("Directory /src is created")
                    project.baseDir.createChildDirectory(this, "src")
                } else {
                    project.baseDir.findFileByRelativePath("src")
                }

                //find or create file
                val targetDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(targetVirtualDir!!)
                try {
                    targetDirectory.add(file)
                    logger.debug("File $name is loaded")
                } catch (e: Exception) {
                    logger.debug("File $name is already loaded")
                }
            }
        }

    }
}