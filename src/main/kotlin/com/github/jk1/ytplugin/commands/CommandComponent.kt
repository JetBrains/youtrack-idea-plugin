package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.ComponentAware
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.util.Key
import java.util.concurrent.Future


interface CommandComponent : ProjectComponent, ComponentAware {

    companion object {
        val COMPONENT_KEY: Key<CommandComponent> = Key.create(CommandComponentImpl::class.toString())
        val SESSION_KEY: Key<CommandSession> = Key.create(CommandSession::class.toString())
    }

    fun executeAsync(execution: YouTrackCommandExecution) : Future<Unit>

    fun suggest(command: YouTrackCommand): CommandAssistResponse

}