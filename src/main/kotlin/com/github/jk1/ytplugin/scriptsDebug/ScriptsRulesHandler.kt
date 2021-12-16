package com.github.jk1.ytplugin.scriptsDebug

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.ScriptsRestClient
import com.github.jk1.ytplugin.timeTracker.TrackerNotification
import com.google.gson.JsonParser
import com.intellij.javascript.debugger.execution.RemoteUrlMappingBean
import com.intellij.lang.javascript.JavaScriptFileType.INSTANCE
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.impl.LoadTextUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.impl.file.PsiDirectoryFactory
import com.intellij.util.IncorrectOperationException


class ScriptsRulesHandler(val project: Project) {

    private var srcDir = project.guessProjectDir()

    private val updatedScriptsNames = mutableListOf<String>()
    private val loadedScriptsNames = mutableListOf<String>()

    private val MODULE_END = "\n});"
    private val MODULE_PREAMBLE = "(function (exports, require, module, __filename, __dirname) {"

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


        scriptsList.map { workflow ->

            // no need to use specific separator
            srcDir = project.guessProjectDir()?.findFileByRelativePath("$rootFolderName/$instanceFolderName")

            // proper directory creating for custom and default scripts
            var scriptDirectory: PsiDirectory? = null
            val scriptDirectories = workflow.name.split('/')
            scriptDirectories.forEach {
                scriptDirectory = createOrFindScriptDirectory(it)
                srcDir = project.guessProjectDir()?.findFileByRelativePath("${srcDir?.path?.drop(project.guessProjectDir()!!.path.length + 1)}/$it")
            }

            logger.debug("Script directory: ${workflow.name}")
            workflow.rules.map { rule ->
                val existingScript = project.guessProjectDir()?.findFileByRelativePath(
                    "$rootFolderName/$instanceFolderName/${workflow.name}/${rule.name}.js"
                )
                if (existingScript != null) {
                    logger.debug("Existing script found: ${existingScript.path}")
                    ScriptsRestClient(repo).getScriptsContent(workflow, rule)
                    logger.debug("existing script content: ${existingScript.contentsToByteArray()}")
                    logger.debug("rule content: ${rule.content.toByteArray()}")

                    if (!LoadTextUtil.loadText(existingScript).toString().equals(rule.content)) {
                        ApplicationManager.getApplication().runWriteAction {
                            existingScript.delete(this)
                            createRuleFile("${rule.name}.js", rule.content, scriptDirectory!!)
                        }
                        updatedScriptsNames.add("${workflow.name}/${rule.name}.js")
                    } else {
                        logger.debug("No changes were made for ${workflow.name}")
                    }
                } else {
                    ScriptsRestClient(repo).getScriptsContent(workflow, rule)
                    createRuleFile("${rule.name}.js", rule.content, scriptDirectory!!)
                    loadedScriptsNames.add("${workflow.name}/${rule.name}.js")
                }
                addScriptMapping(workflow.name, rule.name, mappings, rootFolderName, instanceFolderName)
            }
        }

        if (updatedScriptsNames.isNotEmpty()){
            trackerNote.notify(
                "Scripts updated: \n ${updatedScriptsNames.joinToString("\n")}",
                NotificationType.INFORMATION
            )
        }

        if (loadedScriptsNames.isNotEmpty()) {
            trackerNote.notify(
                "Scripts loaded: \n ${loadedScriptsNames.joinToString("\n")}",
                NotificationType.INFORMATION
            )
        }

        if (scriptsList.isNullOrEmpty()){
            val note = "The debug operation requires that you have permission to update at least one project in " +
                    "YouTrack and have at least one custom workflow or updated predefined workflow "
            trackerNote.notify(note, NotificationType.ERROR)
        }
    }

    private fun addScriptMapping(workflowName: String, ruleName: String, mappings: MutableList<RemoteUrlMappingBean>,
                                    rootFolderName: String, instanceFolderName: String){
        val local = project.guessProjectDir()?.path + "/$rootFolderName/$instanceFolderName/$workflowName/$ruleName.js"

        val localUrls = mutableListOf<String>()
        mappings.forEach { entry -> localUrls.add(entry.localFilePath) }

        if (!localUrls.contains(local)) {
            logger.debug("Mapping added for pair: $local and $instanceFolderName/$workflowName/$ruleName.js")
            mappings.add(RemoteUrlMappingBean(local, "$instanceFolderName/$workflowName/$ruleName.js"))
        }
    }

    private fun createRuleFile(name: String, text: String?, directory: PsiDirectory) {
            ApplicationManager.getApplication().invokeAndWait {
                val psiFileFactory = PsiFileFactory.getInstance(project)

                ApplicationManager.getApplication().runWriteAction {
                    //find or create file
                    try {
                        val file: PsiFile = psiFileFactory.createFileFromText(name, INSTANCE, text as CharSequence)
                        logger.debug("Attempt to load file $name")

                        directory.add(file)
                        makeLoadedFileReadOnly(directory, name)

                        logger.debug("File $name is loaded")
                    } catch (e: IncorrectOperationException) {
                        logger.debug("Most likely file $name is already loaded: ", e)
                    } catch (e: AssertionError) {
                        logger.debug("Most likely The $name file contains unsupported line separators and was not imported from YouTrack", e)

                        val note = "The $name file contains unsupported line separators and was not imported from YouTrack"
                        val trackerNote = TrackerNotification()
                        trackerNote.notify(note, NotificationType.WARNING)

                        val file: PsiFile = psiFileFactory.createFileFromText(
                            name, INSTANCE,
                            "The source script appears to contain unsupported line separators. Please enter the content manually." as CharSequence
                        )
                        try {
                            logger.debug("The $name file contains unsupported line separators and was not imported from YouTrack")
                            directory.add(file)
                        } catch (e: IncorrectOperationException) {
                            logger.debug("Most likely file $name was already loaded", e)
                        }
                    }
                }
            }
    }

    private fun makeLoadedFileReadOnly(directory: PsiDirectory, name: String) {
        directory.findFile(name)?.virtualFile?.isWritable = false
    }

    private fun createOrFindScriptDirectory(name: String): PsiDirectory {
        var targetDirectory: PsiDirectory? = null

        ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
                // find or create directory
                val targetVirtualDir = if (srcDir?.findFileByRelativePath(name) == null) {
                    logger.debug("Directory $name is created")
                    srcDir?.createChildDirectory(this, name)
                } else {
                    srcDir?.findFileByRelativePath(name)
                }
                targetDirectory = PsiDirectoryFactory.getInstance(project).createDirectory(targetVirtualDir!!)
            }
        }
        logger.debug("Directory created: $name")
        return targetDirectory!!
    }


    fun handleScriptsSourcesMessages(message: String): String{
        return try {
            val msgResult = JsonParser.parseString(message).asJsonObject.get("result").asJsonObject
            val content = msgResult.get("scriptSource").asString
            val newContent = removeRedundantStringsFormScriptContent(content)
            logger.debug("Handled scripts sources message: ${message.substring(50)}...")
            message.replace(content, newContent)
        } catch (e: Exception) {
            logger.debug("Failed to handle scripts sources message $message")
            logger.debug(e)
            message
        }
    }

    private fun removeRedundantStringsFormScriptContent(content: String): String {
        return content.removePrefix(MODULE_PREAMBLE).removeSuffix(MODULE_END)
    }
}