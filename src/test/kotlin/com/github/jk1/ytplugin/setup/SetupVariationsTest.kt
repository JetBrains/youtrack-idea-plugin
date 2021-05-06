package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.*
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SetupVariationsTest : IssueRestTrait, IdeaProjectTrait, SetupConnectionTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture
    override lateinit var repository: YouTrackServer
    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
    }

    @Test
    fun `test if connected repository has an issue that can be displayed`() {
        val serverUrl = "https://ytplugintest.myjetbrains.com/youtrack"
        repository = createYouTrackRepository(serverUrl, token)
        repository.defaultSearch = "project: AT"
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()
        val issueId = createIssue()
        try {
            setupTask.testConnection(repo, project)
            issueStoreComponent[repository].update(repository).waitFor(5000)

            assertEquals(NotifierState.SUCCESS, setupTask.noteState)
            assertEquals(1, issueStoreComponent[repository].getAllIssues().size)
        } finally {
            deleteIssue(issueId)
        }
    }

    @Test
    fun `test login anonymously feature`() {
        val serverUrl = "https://ytplugintest.myjetbrains.com/youtrack"
        repository = createYouTrackRepository(serverUrl, token, loginAnon = true)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)
        assertEquals(NotifierState.SUCCESS, setupTask.noteState)

    }

    @Test
    fun `test login anonymously feature with invalid url`() {
        val serverUrl = "https://ytplugintest"
        repository = createYouTrackRepository(serverUrl, token, loginAnon = true)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals(NotifierState.LOGIN_ERROR, setupTask.noteState)
    }

    @Test
    fun `test share url feature`() {
        val serverUrl = "https://ytplugintest.myjetbrains.com/youtrack"
        repository = createYouTrackRepository(serverUrl, token, shareUrl = true)
        val repo = repository.getRepo()
        val setupTask = SetupRepositoryConnector()

        setupTask.testConnection(repo, project)

        assertEquals(NotifierState.SUCCESS, setupTask.noteState)
    }


    //TODO: add case of null host

    @After
    fun tearDown() {
        issueStoreComponent.remove(repository)
        cleanUpTaskManager()
        fixture.tearDown()
    }
}