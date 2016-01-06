package com.github.jk1.ytplugin.model

import com.intellij.tasks.Task
import java.util.*


class YouTrackCommand(

        val command: String,
        val caret: Int = 0,
        val silent: Boolean = false,
        val issues: MutableList<Task> = ArrayList()) {

}