package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.model.CommandAssistResponse
import com.github.jk1.ytplugin.model.YouTrackCommand
import com.github.jk1.ytplugin.model.YouTrackCommandExecution
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.util.Key


interface CommandComponent : ProjectComponent, ComponentAware {

    companion object {
        val USER_DATA_KEY = Key.create<CommandComponent>(CommandComponentImpl::class.toString())
    }

    fun executeAsync(execution: YouTrackCommandExecution)

    fun suggest(command: YouTrackCommand): CommandAssistResponse

}