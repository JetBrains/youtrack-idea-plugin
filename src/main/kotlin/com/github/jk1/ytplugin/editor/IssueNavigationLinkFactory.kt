package com.github.jk1.ytplugin.editor

import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.vcs.IssueNavigationLink

/**
 * Enhances default issue navigation links to recognize actual YouTrack projects from a remote server.
 * That is, for "Shiny Project" - SP and "Money Maker" - MM projects it would only match (SP|MM)-\\d+
 */
object IssueNavigationLinkFactory {

    private const val markerSuffix = "#YouTrack"

    /**
     * Constructs dumb and fast issue navigation link, which matches everything looking like issue id
     */
    fun createNavigationLink(youtrackUrl: String): IssueNavigationLink {
        val link = IssueNavigationLink()
        link.linkRegexp = "$youtrackUrl/issue/$0"
        link.issueRegexp = "(?x)[A-Z]+-\\d+$markerSuffix"
        return link
    }

    /**
     * Makes navigation link smarter and slower by teaching it actual YouTrack project short names
     */
    fun IssueNavigationLink.setProjects(projectShortNames: List<String>){
        issueRegexp = "(?x)(${projectShortNames.joinToString("|")})-\\d+$markerSuffix"
    }

    val IssueNavigationLink.createdByYouTrackPlugin: Boolean
        get() = issueRegexp.endsWith(markerSuffix)

    fun IssueNavigationLink.pointsTo(repo: YouTrackServer) = linkRegexp.startsWith(repo.url)
}