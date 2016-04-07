package com.github.jk1.ytplugin.commands.components

import com.github.jk1.ytplugin.commands.model.CommandAssistResponse
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.common.components.ComponentAware
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.util.Key
import java.util.concurrent.Future


interface CommandComponent : ProjectComponent, ComponentAware {

    companion object {
        val USER_DATA_KEY = Key.create<CommandComponent>(CommandComponentImpl::class.toString())
    }

    fun executeAsync(execution: YouTrackCommandExecution) : Future<*>

    fun suggest(command: YouTrackCommand): CommandAssistResponse

}