package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.notifications.IdeNotificationsTrait
import com.github.jk1.ytplugin.rest.AdminRestClient
import com.github.jk1.ytplugin.rest.CommandRestClient
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.util.concurrency.FutureResult
import java.util.concurrent.Future

@Service
class CommandService(override val project: Project): ICommandService, ComponentAware, IdeNotificationsTrait {

    companion object {
        val SERVICE_KEY: Key<ICommandService> = Key.create(CommandService::class.toString())
        val SESSION_KEY: Key<CommandSession> = Key.create(CommandSession::class.toString())
    }

    private val assistCache = CommandSuggestResponseCache(project)

    override fun executeAsync(execution: YouTrackCommandExecution): Future<Unit> {
        val future = FutureResult<Unit>()
        object : Task.Backgroundable(project, "Executing YouTrack command") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = title
                    val result = execution.session.restClient.executeCommand(execution)
                    result.errors.forEach {
                        showError("Command execution error", it)
                    }
                    result.messages.forEach {
                        showNotification("YouTrack server message", it)
                    }
                } catch(e: Throwable) {
                    showError("Command execution error", e.message ?: "")
                    logger.error("Command execution error", e)
                } finally {
                    future.set(Unit)
                }
            }
        }.queue()
        return future
    }

    override fun suggest(command: YouTrackCommand): CommandAssistResponse {
        val response = assistCache[command] ?: command.session.restClient.assistCommand(command)
        assistCache[command] = response
        return response
    }

    override fun getActiveTaskVisibilityGroups(issue: Issue, callback: (List<String>) -> Unit): Future<Unit> {
        val future = FutureResult<Unit>()
        object : Task.Backgroundable(project, "Loading eligible visibility groups") {
            override fun run(indicator: ProgressIndicator) {
                try {
                    indicator.text = title
                    val repo = taskManagerComponent.getYouTrackRepository(issue)
                    // replaced issueId with entityId
                    issue.entityId.let { AdminRestClient(repo).getVisibilityGroups(it) }.let { callback.invoke(it) }
                } catch (e: Throwable) {
                    logger.info("Failed to load eligible visibility groups for issue")
                    logger.debug(e)
                } finally {
                    future.set(Unit)
                }
            }
        }.queue()
        return future
    }

    private val CommandSession.restClient: CommandRestClient
        get() = CommandRestClient(taskManagerComponent.getYouTrackRepository(issue))
}