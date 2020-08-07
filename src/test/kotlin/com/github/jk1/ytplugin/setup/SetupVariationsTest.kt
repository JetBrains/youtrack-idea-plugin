package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.SetupConnectionTrait
import com.github.jk1.ytplugin.SetupManagerTrait
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SetupVariationsTest : SetupManagerTrait, IdeaProjectTrait, SetupConnectionTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture
    override lateinit var repository: YouTrackServer
    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
    }

    @Test
    fun `test if connected repository has 3 issues that can be displayed`() {
        val serverUrl = "https://ytplugintest.myjetbrains.com/youtrack"
        repository = createYouTrackRepository(serverUrl, token)
        repository.defaultSearch = "Assignee:Unassigned"
        val repo = repository.getRepo()
        val setupTask = SetupManager()

        setupTask.testConnection(repo, project)
        issueStoreComponent[repository].update(repository).waitFor(5000)

        assertEquals(NotifierState.SUCCESS, setupTask.noteState)
        assertEquals(3, issueStoreComponent[repository].getAllIssues().size)
    }

    @Test
    fun `test login anonymously feature`() {
        val serverUrl = "https://ytplugintest.myjetbrains.com/youtrack"
        repository = createYouTrackRepository(serverUrl, token, loginAnon = true)
        val repo = repository.getRepo()
        val setupTask = SetupManager()

        setupTask.testConnection(repo, project)

        assertEquals(200, setupTask.statusCode)
    }

    @Test
    fun `test login anonymously feature with invalid url`() {
        val serverUrl = "https://ytplugintest"
        repository = createYouTrackRepository(serverUrl, token, loginAnon = true)
        val repo = repository.getRepo()
        val setupTask = SetupManager()

        setupTask.testConnection(repo, project)

        assertEquals(NotifierState.UNKNOWN_HOST, setupTask.noteState)
        assertEquals(401, setupTask.statusCode)
    }

    @Test
    fun `test share url feature`() {
        val serverUrl = "https://ytplugintest.myjetbrains.com/youtrack"
        repository = createYouTrackRepository(serverUrl, token, shareUrl = true)
        val repo = repository.getRepo()
        val setupTask = SetupManager()

        setupTask.testConnection(repo, project)

        assertEquals(NotifierState.SUCCESS, setupTask.noteState)
        assertEquals(200, setupTask.statusCode)
    }

    @Test
    fun `test use HTTP feature`() {
        val serverUrl = "https://ytplugintest.myjetbrains.com/youtrack"
        repository = createYouTrackRepository(serverUrl, token, useHTTP = true)
        val repo = repository.getRepo()
        val setupTask = SetupManager()

        setupTask.testConnection(repo, project)

        assertEquals(NotifierState.SUCCESS, setupTask.noteState)
        assertEquals(200, setupTask.statusCode)
    }

    @After
    fun tearDown() {
        issueStoreComponent.remove(repository)
        cleanUpTaskManager()
        fixture.tearDown()
    }
}