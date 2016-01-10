package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.model.YouTrackCommand
import com.intellij.openapi.components.ProjectComponent
import com.intellij.tasks.youtrack.YouTrackIntellisense


interface CommandComponent : ProjectComponent {

    fun executeAsync(command: YouTrackCommand)

    fun getIntellisense() : YouTrackIntellisense
}