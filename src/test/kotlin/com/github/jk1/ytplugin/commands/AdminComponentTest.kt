package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.IssueRestTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AdminComponentTest : IdeaProjectTrait, IssueRestTrait, TaskManagerTrait {

    lateinit var fixture: IdeaProjectTestFixture
    lateinit var server: YouTrackServer
    lateinit var issue: String
    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        server = createYouTrackRepository()
        server.defaultSearch = "project: AT"
        issue = createIssue()
        val localTask = server.findTask(issue)!!
        readAction { getTaskManagerComponent().activateTask(localTask, true) }
    }

    @Test
    fun getVisibilityGroups() {
        adminComponent.getActiveTaskVisibilityGroups({ groups ->
            assertEquals(3, groups.size)
            assertTrue(groups.contains("All Users"))
            assertTrue(groups.contains("Registered Users"))
            assertTrue(groups.contains("Automated Test-team"))
        }).get()
    }

    @After
    fun tearDown() {
        deleteIssue(issue)
        cleanUpTaskManager()
        fixture.tearDown()
    }
}