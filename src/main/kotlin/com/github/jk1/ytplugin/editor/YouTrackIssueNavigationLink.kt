package com.github.jk1.ytplugin.editor

import com.intellij.openapi.vcs.IssueNavigationLink

/**
 * Smart navigation link, which affects only known youtrack projects.
 * That is, for "Shiny Project" - SP and "Money Maker" - MM projects it would only match (SP|MM)-\\d+
 *
 * Primary noarg constructor exists to support serialization
 */
class YouTrackIssueNavigationLink() : IssueNavigationLink() {

    constructor(youtrackUrl: String) : this() {
        linkRegexp = "$youtrackUrl/issue/$0"
    }

    fun setProjects(projectShortNames: List<String>){
        issueRegexp = "(${projectShortNames.joinToString("|")})-\\d+"
    }
}