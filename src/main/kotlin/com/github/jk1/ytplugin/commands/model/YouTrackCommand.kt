package com.github.jk1.ytplugin.commands.model

import com.github.jk1.ytplugin.commands.CommandSession

data class YouTrackCommand(

        val session: CommandSession,
        val command: String,
        val caret: Int = 0) {}

data class YouTrackCommandExecution(

        val session: CommandSession,
        val command: String,
        val silent: Boolean = false,
        val comment: String? = null,
        val commentVisibleGroup: String) {} // null means 'All Users' in YouTrack
