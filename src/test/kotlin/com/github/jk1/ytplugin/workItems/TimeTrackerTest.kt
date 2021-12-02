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
import com.github.jk1.ytplugin.timeTracker.actions.SaveTrackerAction
import com.intellij.openapi.project.Project
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
    fun `test can't post new work item with time = 0`() {
        val wiSize = UserRestClient(repository).getWorkItemsForUser().size
        val storedIssues = issueStoreComponent[repository].getAllIssues()
        val id = storedIssues[0].id

        TimeTrackerRestClient(repository).postNewWorkItem(id, "0","Testing", "test item", Date().time.toString())

        assertEquals(wiSize, UserRestClient(repository).getWorkItemsForUser().size)
    }

    @Test
    fun `test can't post new work item with time time less than 0`() {
        val wiSize = UserRestClient(repository).getWorkItemsForUser().size
        val storedIssues = issueStoreComponent[repository].getAllIssues()
        val id = storedIssues[0].id

        TimeTrackerRestClient(repository).postNewWorkItem(id, "-1","Testing", "test item", Date().time.toString())

        assertEquals(wiSize, UserRestClient(repository).getWorkItemsForUser().size)
    }

    @Test
    fun `test post new work item with invalid issue id`() {
        val wiSize = UserRestClient(repository).getWorkItemsForUser().size
        val id = ""

        TimeTrackerRestClient(repository).postNewWorkItem(id, "1","Testing", "test item", Date().time.toString())

        assertEquals(wiSize, UserRestClient(repository).getWorkItemsForUser().size)
    }


    @Test
    fun `test save work item functionality`() {

        val storedIssues = issueStoreComponent[repository].getAllIssues()
        val timer = timeTrackerComponent
        timeTrackerComponent.type = "Testing"

        val id1 = storedIssues[0].id
        timer.start(id1)
        TimeUnit.SECONDS.sleep(65L)
        timer.pause("pause")
        SaveTrackerAction().saveTimer(project, id1)


        val id2 = storedIssues[1].id
        timer.start(id2)
        TimeUnit.SECONDS.sleep(65L)
        timer.pause("pause")
        SaveTrackerAction().saveTimer(project, id2)

        assertEquals(spentTimePerTaskStorage.getAllStoredItems().size, 2)
    }

    @Test
    fun `test saving the same work item several times`() {

        val storedIssues = issueStoreComponent[repository].getAllIssues()
        val timer = timeTrackerComponent
        timeTrackerComponent.type = "Testing"

        val id = storedIssues[0].id
        timer.start(id)
        TimeUnit.SECONDS.sleep(65L)
        timer.pause("pause")
        SaveTrackerAction().saveTimer(project, id)

        timer.start(id)
        TimeUnit.SECONDS.sleep(65L)
        timer.pause("pause")
        SaveTrackerAction().saveTimer(project, id)

        assertEquals(spentTimePerTaskStorage.getAllStoredItems().size, 1)
        //  x / 130000 is needed to take into account milliseconds for operations execution
        assertEquals(spentTimePerTaskStorage.getSavedTimeForLocalTask(id) / 130000 , 1)

    }

    @Test
    fun `test post previously saved work item`() {
        val wiSize = UserRestClient(repository).getWorkItemsForUser().size
        val storedIssues = issueStoreComponent[repository].getAllIssues()

        // saved 2 min for issue
        spentTimePerTaskStorage.setSavedTimeForLocalTask(storedIssues[0].id, 120000)
        timeTrackerComponent.type = "Testing"

        val item: ConcurrentHashMap<String, Long> = ConcurrentHashMap()
        item[storedIssues[0].id] = spentTimePerTaskStorage.getSavedTimeForLocalTask(storedIssues[0].id)
        TimeTrackerConnector(repository, project).postSavedWorkItemsToServer(item)

        assertEquals(wiSize + 1, UserRestClient(repository).getWorkItemsForUser().size)
        assertEquals(spentTimePerTaskStorage.getAllStoredItems().size, 0)
    }

    @Test
    fun `test post previously saved multiple work items`() {
        val wiSize = UserRestClient(repository).getWorkItemsForUser().size
        val storedIssues = issueStoreComponent[repository].getAllIssues()

        // saved 2 min for issue
        spentTimePerTaskStorage.setSavedTimeForLocalTask(storedIssues[0].id, 120000)
        spentTimePerTaskStorage.setSavedTimeForLocalTask(storedIssues[1].id, 180000)

        timeTrackerComponent.type = "Testing"

        val items: ConcurrentHashMap<String, Long> = ConcurrentHashMap()
        items[storedIssues[0].id] = spentTimePerTaskStorage.getSavedTimeForLocalTask(storedIssues[0].id)
        items[storedIssues[1].id] = spentTimePerTaskStorage.getSavedTimeForLocalTask(storedIssues[1].id)

        TimeTrackerConnector(repository, project).postSavedWorkItemsToServer(items)

        assertEquals(wiSize + 2, UserRestClient(repository).getWorkItemsForUser().size)
        assertEquals(spentTimePerTaskStorage.getAllStoredItems().size, 0)
    }

    @After
    fun tearDown() {
        issueStoreComponent.remove(repository)
        deleteIssue(issue.id)
        cleanUpTaskManager()
        spentTimePerTaskStorage.removeAllSavedItems()
        fixture.tearDown()
    }
}