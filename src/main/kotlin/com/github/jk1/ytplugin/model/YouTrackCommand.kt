package com.github.jk1.ytplugin.model


class YouTrackCommand(

        val command: String,
        val caret: Int = 0,
        val issues: Iterable<YouTrackIssue>) {

}