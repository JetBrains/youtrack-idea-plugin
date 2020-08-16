package com.github.jk1.ytplugin.editor

import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.IssueNavigationConfiguration
import com.intellij.openapi.vcs.IssueNavigationLink
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NavigationLinksTest : IdeaProjectTrait, TaskManagerTrait {

    private lateinit var fixture: IdeaProjectTestFixture

    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        // post startup activities aren't called in the test mode at all
        IssueLinkProviderExtension().runActivity(project)
    }

    @Test
    fun issueLinksSetUpForProject(){
        val comment = "// this line fixes AT-18"
        val navigationConfig = IssueNavigationConfiguration.getInstance(project)
        assertTrue(navigationConfig.findIssueLinks(comment).isEmpty())

        setUpYouTrackServer()
        Thread.sleep(5000) // wait for async configuration listeners to be notified

        assertTrue(navigationConfig.findIssueLinks(comment).map { it.targetUrl }.contains("$serverUrl/issue/AT-18"))
    }

    @Test
    fun noLinksForUnknownProject(){
        val comment = "// this line also fixes WTF-42"
        setUpYouTrackServer()
        Thread.sleep(5000) // wait for async configuration listeners to be notified

        assertTrue(IssueNavigationConfiguration.getInstance(project).findIssueLinks(comment).isEmpty())
    }

    @Test
    fun manualConfigurationShouldNotBeOverridden(){
        val comment = "// this line fixes AT-18"
        val navigationConfig = IssueNavigationConfiguration.getInstance(project)
        navigationConfig.links.add(IssueNavigationLink("[az]--//w", "$serverUrl/issue/$0"))

        setUpYouTrackServer()
        Thread.sleep(5000) // wait for async configuration listeners to be notified

        assertTrue(navigationConfig.findIssueLinks(comment).isEmpty())
    }

    private fun setUpYouTrackServer() : YouTrackServer {
        val server = createYouTrackRepository()
        server.defaultSearch = "project: AT"
        return server
    }

    @After
    fun tearDown() {
        IssueNavigationConfiguration.getInstance(project).links = listOf()
        cleanUpTaskManager()
        fixture.tearDown()
    }
}