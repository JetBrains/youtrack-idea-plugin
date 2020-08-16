package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.issues.model.Issue
import java.util.concurrent.Future


interface ICommandService : ComponentAware {

    fun executeAsync(execution: YouTrackCommandExecution) : Future<Unit>

    fun suggest(command: YouTrackCommand): CommandAssistResponse

    fun getActiveTaskVisibilityGroups(issue: Issue, callback: (List<String>) -> Unit): Future<Unit>

}