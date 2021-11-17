package com.github.jk1.ytplugin.workItems

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.IssueRestTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.rest.UserRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.github.jk1.ytplugin.timeTracker.TimeTrackerConnector
import com.intellij.openapi.project.Project
import com.intellij.tasks.LocalTask
import com.intellij.tasks.impl.LocalTaskImpl
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class TimeTrackerTest : IssueRestTrait, IdeaProjectTrait, TaskManagerTrait, ComponentAware {

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
    fun `time tracking service is accessible once the plugin is initialized`() {
        // fails on inconsistent plugin.xml, incomplete classpath and so on
        with (ComponentAware.of(fixture.project)){
            Assert.assertNotNull(timeTrackerComponent)
        }
    }

    @Test
    fun `test update with zero work items`() {
        val storedIWorkItems = issueWorkItemsStoreComponent[repository].getAllWorkItems()
        assertEquals(0, storedIWorkItems.size)
    }

    @Test
    fun `test start timer functionality`() {
        val timer = timeTrackerComponent
        assertEquals(timer.isRunning, false)
        val storedIssues = issueStoreComponent[repository].getAllIssues()
        val id = storedIssues[0].id
        timer.start(id)
        assertEquals(timer.isRunning, true)
    }

    @Test
    fun `test pause timer functionality`() {
        val timer = timeTrackerComponent
        val storedIssues = issueStoreComponent[repository].getAllIssues()
        val id = storedIssues[0].id
        timer.start(id)
        assertEquals(timer.isRunning, true)
        assertEquals(timer.isPaused, false)
        timer.pause("Work timer paused")
        assertEquals(timer.isRunning, true)
        assertEquals(timer.isPaused, true)
    }

    @Test
    fun `test reset timer functionality`() {
        val timer = timeTrackerComponent
        val storedIssues = issueStoreComponent[repository].getAllIssues()
        val id = storedIssues[0].id
        timer.start(id)
        TimeUnit.MINUTES.sleep(1L)
        timer.reset()
        assertEquals(timer.isRunning, true)
        assertEquals(timer.isPaused, false)
        assertEquals(timer.recordedTime, "0")
    }

    @Test
    fun `test post new work item`() {
        val wiSize = UserRestClient(repository).getWorkItemsForUser().size
        val storedIssues = issueStoreComponent[repository].getAllIssues()
        val id = storedIssues[0].id

        TimeTrackerRestClient(repository).postNewWorkItem(id, "20","Testing", "test item", Date().time.toString())

        assertEquals(wiSize + 1, UserRestClient(repository).getWorkItemsForUser().size)
    }

    @Test
    fun `test post previously saved work item`() {
        val wiSize = UserRestClient(repository).getWorkItemsForUser().size
        val storedIssues = issueStoreComponent[repository].getAllIssues()


        // saved 2 min for active task
        spentTimePerTaskStorage.setSavedTimeForLocalTask(storedIssues[0].id, 120000)
        timeTrackerComponent.type = "Testing"

        val item: ConcurrentHashMap<String, Long> = ConcurrentHashMap()
        item[storedIssues[0].id] = spentTimePerTaskStorage.getSavedTimeForLocalTask(storedIssues[0].id)

        TimeTrackerConnector().postSavedTimeToServer(repository, project, item)
        assertEquals(wiSize + 1, UserRestClient(repository).getWorkItemsForUser().size)
        assertEquals(spentTimePerTaskStorage.getAllStoredItems().size, 0)

    }

    @After
    fun tearDown() {
        issueStoreComponent.remove(repository)
        deleteIssue(issue.id)
        cleanUpTaskManager()
        fixture.tearDown()
    }
}