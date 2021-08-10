package com.github.jk1.ytplugin.scriptsDebug

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.ScriptsRestClient
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.intellij.javascript.debugger.execution.RemoteUrlMappingBean
import com.intellij.lang.javascript.JavaScriptFileType.INSTANCE
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.intellij.util.IncorrectOperationException


class ScriptsRulesHandler(val project: Project) {

    private var srcDir = project.baseDir

    fun loadWorkflowRules(mappings: MutableList<RemoteUrlMappingBean>, rootFolderName: String, instanceFolderName: String) {
        val repositories = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories()
        val repo = if (repositories.isNotEmpty()) {
            repositories.first()
        } else null

        val scriptsList = ScriptsRestClient(repo!!).getScriptsWithRules()
        val trackerNote = TrackerNotification()

        createOrFindScriptDirectory(rootFolderName)
        srcDir = project.guessProjectDir()?.findFileByRelativePath(rootFolderName)

        createOrFindScriptDirectory(instanceFolderName)
        srcDir = project.guessProjectDir()?.findFileByRelativePath("$rootFolderName/$instanceFolderName")

        createOrFindScriptDirectory("@jetbrains")
        srcDir = project.guessProjectDir()?.findFileByRelativePath("$rootFolderName/$instanceFolderName/@jetbrains")

        scriptsList.map { workflow ->
            val scriptDirectory = createOrFindScriptDirectory(workflow.name.split('/').last())
            workflow.rules.map { rule ->
                val existingScript = project.guessProjectDir()?.findFileByRelativePath(
                    "$rootFolderName/$instanceFolderName/@jetbrains/${workflow.name.split('/').last()}/${rule.name}.js"
                )
                if (existingScript == null) {
                    ScriptsRestClient(repo).getScriptsContent(workflow, rule)
                    createRuleFile("${rule.name}.js", rule.content, scriptDirectory)
                    val local = project.guessProjectDir()?.path +
                        "/$rootFolderName/$instanceFolderName/@jetbrains/${workflow.name.split('/').last()}/${rule.name}.js"


                    val localUrls = mutableListOf<String>()
                    mappings.forEach { entry -> localUrls.add(entry.localFilePath) }

                    if (!localUrls.contains(local)){
                        mappings.add(RemoteUrlMappingBean(local, "scripts/${workflow.name}/${rule.name}.js"))
                    }

                    trackerNote.notify(
                        "Script loaded \"${workflow.name}\"",
                        NotificationType.INFORMATION
                    )
                }
            }
        }
    }

    private fun createRuleFile(name: String, text: String?, directory: PsiDirectory) {
            ApplicationManager.getApplication().invokeAndWait {
                val psiFileFactory = PsiFileFactory.getInstance(project)

                ApplicationManager.getApplication().runWriteAction {
                    //find or create file
                    try {
                        val file: PsiFile = psiFileFactory.createFileFromText(name, INSTANCE, text as CharSequence)
                        logger.info("Attempt to load file $name")
                        directory.add(file)
                        logger.info("File $name is loaded")
                    } catch (e: IncorrectOperationException) {
                        logger.info("File $name is already loaded")
                    } catch (e: AssertionError) {
                        val note = "The $name file contains unsupported line separators and was not imported from YouTrack"
                        val trackerNote = TrackerNotification()
                        trackerNote.notify(note, NotificationType.WARNING)

                        val file: PsiFile = psiFileFactory.createFileFromText(
                            name, INSTANCE,
                            "The source script appears to contain unsupported line separators. Please enter the content manually." as CharSequence
                        )
                        directory.add(file)
                        logger.info("The $name file contains unsupported line separators and was not imported from YouTrack")
                    }
                }
            }
    }

    private fun createOrFindScriptDirectory(name: String): PsiDirectory {
        var targetDirectory: PsiDirectory? = null

        ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
                // find or create directory
                val targetVirtualDir = if (srcDir?.findFileByRelativePath(name) == null) {
                    logger.info("Directory $name is created")
                    srcDir?.createChildDirectory(this, name)
                } else {
                    srcDir.findFileByRelativePath(name)
                }
                targetDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(targetVirtualDir!!)
            }
        }
        return targetDirectory!!
    }
}