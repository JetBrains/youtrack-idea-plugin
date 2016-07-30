package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.IssueRestTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.github.jk1.ytplugin.common.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

class IssueStoreComponentTest : IssueRestTrait, IdeaProjectTrait, TaskManagerTrait {

    lateinit var fixture: IdeaProjectTestFixture
    lateinit var server: YouTrackServer
    override val project: Project by lazy { fixture.project }
    val issues = ArrayList<String>()

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        issues.add(createIssue())
        server = createYouTrackRepository()
        server.defaultSearch = "project: AT"
    }

    @Test
    fun testStoreLoad() {
        issueStoreComponent[server].update().waitFor(5000)
        Assert.assertEquals(1, issueStoreComponent[server].getAllIssues().size)
    }

    @After
    fun tearDown() {
        issues.forEach { deleteIssue(it) }
        cleanUpTaskManager()
        fixture.tearDown()
    }
}