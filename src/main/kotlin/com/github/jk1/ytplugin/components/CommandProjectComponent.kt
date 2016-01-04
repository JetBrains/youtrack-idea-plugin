package com.github.jk1.ytplugin.components

import com.github.jk1.ytplugin.model.YouTrackCommand
import com.github.jk1.ytplugin.model.YouTrackParsedCommand
import com.intellij.openapi.components.ProjectComponent


interface CommandProjectComponent : ProjectComponent {

    fun execute(command: YouTrackCommand)
    fun parse(command: YouTrackCommand): YouTrackParsedCommand
}