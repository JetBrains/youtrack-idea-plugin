package com.github.jk1.ytplugin.commands.model

import com.github.jk1.ytplugin.issues.model.Issue

data class YouTrackCommand(

        val issue: Issue,
        val command: String,
        val caret: Int = 0)

data class YouTrackCommandExecution(

        val issue: Issue,
        val command: String,
        val silent: Boolean = false,
        val comment: String? = null,
        val commentVisibleGroup: String)  // null means 'All Users' in YouTrack
