package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class PluginApiTest : IssueRestTrait, IdeaProjectTrait, TaskManagerTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture
    private lateinit var issue: Issue

    override lateinit var repository: YouTrackServer
    override val project: Project by lazy { fixture.project }

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
    fun testIssueSearch() {
        val issues = pluginApiComponent.search(issue.id)

        assertEquals(1, issues.size)
        assertEquals(issue.id, issues.first().issueId)
    }

    @Test
    fun testCommandExecution() {
        pluginApiComponent.executeCommand(issue, "Fixed")

        assertTrue(repository.getTasks(issue.id, 0, 1).first().isClosed)
    }

    @After
    fun tearDown() {
        deleteIssue(issue.id)
        cleanUpTaskManager()
        fixture.tearDown()
    }
}