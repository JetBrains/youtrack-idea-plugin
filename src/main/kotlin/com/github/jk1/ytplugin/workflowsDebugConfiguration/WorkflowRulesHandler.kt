package com.github.jk1.ytplugin.workflowsDebugConfiguration

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.WorkflowsRestClient
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileTypes.StdFileTypes.JS
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
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
                        val scriptDirectory = createScriptDirectory(project, workflow.name.split('/').last())
                        for (rule in workflow.rules) {
                            WorkflowsRestClient(repo).getWorkFlowContent(workflow, rule)
                            createScriptFile("${rule.name}.js", rule.content, project, scriptDirectory)
                        }
                        trackerNote.notify("Successfully loaded workflow \"${workflow.name}\"", NotificationType.INFORMATION)
                    }
                })
    }

    private fun createScriptFile(name: String, text: String?, project: Project, directory: PsiDirectory) {

        ApplicationManager.getApplication().invokeLater {

            val psiFileFactory = PsiFileFactory.getInstance(project)
            val file: PsiFile = psiFileFactory.createFileFromText(name, JS, text as @NotNull @NonNls CharSequence)

            ApplicationManager.getApplication().runWriteAction {
                //find or create file
                try {
                    directory.add(file)
                    logger.debug("File $name is loaded")
                } catch (e: Exception) {
                    logger.debug("File $name is already loaded")
                }
            }
        }

    }

    private fun createScriptDirectory(project: Project, name: String) : PsiDirectory {
        var targetDirectory: PsiDirectory? = null

        ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
                // find or create directory
                val targetVirtualDir = if (project.baseDir.findFileByRelativePath(name) == null) {
                    logger.debug("Directory $name is created")
                    project.baseDir.createChildDirectory(this, name)
                } else {
                    project.baseDir.findFileByRelativePath(name)
                }

                targetDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(targetVirtualDir!!)
            }
        }
        return targetDirectory!!

    }
}