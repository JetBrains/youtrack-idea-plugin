package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.IssueRestTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdminComponentTest : IdeaProjectTrait, IssueRestTrait, TaskManagerTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture
    private lateinit var issue: Issue

    override val project: Project by lazy { fixture.project }
    override lateinit var repository: YouTrackServer

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        repository = createYouTrackRepository()
        repository.defaultSearch = "project: AT"
        createIssue()
        issueStoreComponent[repository].update(repository).waitFor(5000)
        issue = issueStoreComponent[repository].getAllIssues().first()
    }

    @Test
    fun getVisibilityGroups() {
        adminComponent.getActiveTaskVisibilityGroups(issue) { groups ->
            assertEquals(3, groups.size)
            assertTrue(groups.contains("All Users"))
            assertTrue(groups.contains("Registered Users"))
            assertTrue(groups.contains("Automated Test-team"))
        }.get()
    }

    @After
    fun tearDown() {
        deleteIssue(issue.id)
        cleanUpTaskManager()
        fixture.tearDown()
    }
}