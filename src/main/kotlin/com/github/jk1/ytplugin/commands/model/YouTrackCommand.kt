package com.github.jk1.ytplugin.commands.model

import com.intellij.tasks.Task
import java.util.*


data class YouTrackCommand(

        val command: String,
        val caret: Int = 0,
        val issues: MutableList<Task> = ArrayList()) {}

data class YouTrackCommandExecution(

        val command: YouTrackCommand,
        val silent: Boolean = false,
        val comment: String? = null,
        val commentVisibleGroup: String) {} // null means 'All Users' in YouTrack
