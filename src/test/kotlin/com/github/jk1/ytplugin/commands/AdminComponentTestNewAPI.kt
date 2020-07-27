package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.IssueRestTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.externalSystem.EMPTY_STATE
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.IssueNavigationLink
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import sun.invoke.empty.Empty

class AdminComponentTestNewAPI : IdeaProjectTrait, IssueRestTrait, TaskManagerTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture
    private lateinit var issue: Issue

    override val project: Project by lazy { fixture.project }
    override lateinit var repository: YouTrackServer

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        repository = createYouTrackRepository()
        issueStoreComponent[repository].update(repository).waitFor(5000)
        issue = issueStoreComponent[repository].getAllIssues().first()
    }

    @Test
    fun `get visibility groups using new API`() {
//        println(issue.id)
        var size = 0
        var group: List<String> = mutableListOf()
        adminComponent.getActiveTaskVisibilityGroups(issue) { groups ->
            size = groups.size
            group = groups
        }.get()

        assertTrue(group.contains("Manual Test-team"))
        assertTrue(group.contains("Manual Test Team"))
        assertTrue(group.contains("Automated Test-team"))
        assertTrue(group.contains("Registered Users"))
        assertTrue(group.contains("All Users"))
        assertEquals(size, 5)
    }

    @Test
    fun `get accessible projects using new API`() {
        val link = IssueNavigationLink()
        adminComponent.updateIssueLinkProjects(link, repository)
//        assertTrue(projects.contains("MT"))
//        assertTrue(projects.contains("AT"))
    }

    @After
    fun tearDown() {
        cleanUpTaskManager()
        fixture.tearDown()
    }
}