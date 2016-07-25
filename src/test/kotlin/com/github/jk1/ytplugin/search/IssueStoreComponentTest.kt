package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.IssueRestTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.intellij.openapi.project.Project
import com.intellij.tasks.impl.BaseRepository
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

class IssueStoreComponentTest : IssueRestTrait, IdeaProjectTrait, TaskManagerTrait {

    lateinit var fixture: IdeaProjectTestFixture
    lateinit var repo: BaseRepository
    override val project: Project by lazy { fixture.project }
    val issues = ArrayList<String>()

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        issues.add(createIssue())
        repo = createYouTrackRepository()
        issueStoreComponent[repo].searchQuery = "project: AT"
    }

    @Test
    fun testStoreLoad() {
        issueStoreComponent[repo].update().waitFor(5000)
        Assert.assertEquals(1, issueStoreComponent[repo].getAllIssues().size)
    }

    @After
    fun tearDown() {
        issues.forEach { deleteIssue(it) }
        cleanUpTaskManager()
        fixture.tearDown()
    }
}