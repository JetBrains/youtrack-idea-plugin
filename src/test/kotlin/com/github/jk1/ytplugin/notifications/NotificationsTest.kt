package com.github.jk1.ytplugin.notifications

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.IssueRestTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.github.jk1.ytplugin.rest.NotificationsRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.*
import org.junit.Assert.assertTrue

class NotificationsTest : IssueRestTrait, IdeaProjectTrait, TaskManagerTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture
    private lateinit var issueId: String

    override lateinit var repository: YouTrackServer
    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        repository = createYouTrackRepository()
        repository.defaultSearch = "project: AT"
        issueId = createIssue()
    }

    @Test
    fun testNotificationFetch(){
        Thread.sleep(60000) // let notification analyzer generate notification
        val notifications = NotificationsRestClient(repository).getNotifications()
        assertTrue(notifications.any { it.issueId == issueId })
    }

    @After
    fun tearDown() {
        deleteIssue(issueId)
        cleanUpTaskManager()
        fixture.tearDown()
    }
}