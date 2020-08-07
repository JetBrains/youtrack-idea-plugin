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

class InputCredentialsTest : SetupManagerTrait, IdeaProjectTrait, SetupConnectionTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture
    override lateinit var repository: YouTrackServer
    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
    }

    @Test
    fun `test connection with HTTP error in url`() {
        val serverUrl = "http://ytplugintest.myjetbrains.com/youtrack"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals("https://ytplugintest.myjetbrains.com/youtrack", setupTask.correctUrl)
        assertEquals(NotifierState.SUCCESS, setupTask.noteState)
        assertEquals(200, setupTask.statusCode)
    }

    @Test
    fun `test connection with ending error in url`() {
        val serverUrl = "https://ytplugintest.myjetbrains.com"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals("https://ytplugintest.myjetbrains.com/youtrack", setupTask.correctUrl)
        assertEquals(NotifierState.SUCCESS, setupTask.noteState)
        assertEquals(200, setupTask.statusCode)
    }

    @Test
    fun `test connection with HTTP and ending error in url`() {
        val serverUrl = "http://ytplugintest.myjetbrains.com"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals("https://ytplugintest.myjetbrains.com/youtrack", setupTask.correctUrl)
        assertEquals(NotifierState.SUCCESS, setupTask.noteState)
        assertEquals(200, setupTask.statusCode)
    }

    @Test
    fun `test connection with trailing slash error in url`() {
        val serverUrl = "https://ytplugintest.myjetbrains.com/youtrack/////"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals("https://ytplugintest.myjetbrains.com/youtrack", setupTask.correctUrl)
        assertEquals(NotifierState.SUCCESS, setupTask.noteState)
        assertEquals(200, setupTask.statusCode)
    }

    @Test
    fun `test connection with invalid token `() {
        val serverUrl = "https://ytplugintest.myjetbrains.com/youtrack/////"
        val token = "RlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals(NotifierState.INVALID_TOKEN, setupTask.noteState)
        assertEquals(401, setupTask.statusCode)
    }

    @Test
    fun `test connection with non-existing url`() {
        val serverUrl = "lug"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals("https://lug/youtrack", setupTask.correctUrl)
        assertEquals(NotifierState.UNKNOWN_HOST, setupTask.noteState)
        assertEquals(401, setupTask.statusCode)
    }

    @Test
    fun `test connection with non-existing url looking like existing`() {
        val serverUrl = "https://tains.com"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals("https://tains.com/youtrack", setupTask.correctUrl)
        assertEquals(NotifierState.LOGIN_ERROR, setupTask.noteState)
        assertEquals(401, setupTask.statusCode)
    }

    @Test
    fun `test connection with empty form`() {
        repository = createYouTrackRepository("", "")
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals("", setupTask.correctUrl)
        assertEquals(NotifierState.INVALID_TOKEN, setupTask.noteState)
        assertEquals(401, setupTask.statusCode)
    }

    @After
    fun tearDown() {
        issueStoreComponent.remove(repository)
        cleanUpTaskManager()
        fixture.tearDown()
    }
}
