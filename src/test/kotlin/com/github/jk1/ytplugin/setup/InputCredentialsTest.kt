package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.SetupConnectionTrait
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class InputCredentialsTest : IdeaProjectTrait, SetupConnectionTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture
    override val project: Project by lazy { fixture.project }

    lateinit var repository: YouTrackServer

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
    }

    @Test
    fun `test connection with protocol error in url`() {
        val myServerUrl = serverUrl.replace("https", "http")
        repository = createYouTrackRepository(myServerUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals(serverUrl, repository.getRepo().url)
        assertEquals(NotifierState.SUCCESS, setupTask.noteState)
    }

    @Test
    fun `test connection with no youtrack ending in url`() {
        val myServerUrl = serverUrlOld.replace("/youtrack", "")
        repository = createYouTrackRepository(myServerUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals(myServerUrl, repository.getRepo().url)
        assertEquals(NotifierState.SUCCESS, setupTask.noteState)
    }

    @Test
    fun `test connection with no youtrack ending and protocol error in url`() {
        val myServerUrl = serverUrlOld.replace("/youtrack", "").replace("https", "http")
        repository = createYouTrackRepository(myServerUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals(myServerUrl.replace("http", "https"), repository.getRepo().url)
        assertEquals(NotifierState.SUCCESS, setupTask.noteState)
    }

    @Test
    fun `test connection with trailing slash error in url`() {
        val myServerUrl = "$serverUrl/////"
        repository = createYouTrackRepository(myServerUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals(serverUrl, repository.getRepo().url)
        assertEquals(NotifierState.SUCCESS, setupTask.noteState)
    }

    @Test
    fun `test connection with invalid token`() {
        val token = "RlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)
        assertEquals(NotifierState.UNAUTHORIZED, setupTask.noteState)
    }

    @Test
    fun `test connection with application password`() {
        repository = createYouTrackRepository(serverUrl, "$username:$applicationPassword")
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)
        assertEquals(NotifierState.SUCCESS, setupTask.noteState)
    }

    @Test
    fun `test connection with application password in wrong format`() {
        repository = createYouTrackRepository(serverUrl, "$username:$applicationPassword 12A3")
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)
        assertEquals(NotifierState.UNAUTHORIZED, setupTask.noteState)
    }

    @Test
    fun `test connection with no protocol`() {
        val myServerUrl = serverUrl.substring(serverUrl.indexOf("://") + 3, serverUrl.lastIndex + 1)
        repository = createYouTrackRepository(myServerUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)
        assertEquals(NotifierState.SUCCESS, setupTask.noteState)
    }

    @Test
    fun `test connection with non-existing url`() {
        val serverUrl = "lug"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals("http://lug", repository.getRepo().url)
        assertEquals(NotifierState.LOGIN_ERROR, setupTask.noteState)
    }

    @Test
    fun `test connection with non-existing url looking like existing`() {
        val serverUrl = "https://tains.com"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals("http://tains.com", repository.getRepo().url)
        assertEquals(NotifierState.LOGIN_ERROR, setupTask.noteState)
    }

    @After
    fun tearDown() {
        issueStoreComponent.remove(repository)
        cleanUpTaskManager()
        fixture.tearDown()
    }
}
