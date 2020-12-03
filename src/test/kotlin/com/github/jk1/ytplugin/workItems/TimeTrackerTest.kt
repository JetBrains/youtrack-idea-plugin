package com.github.jk1.ytplugin.workItems

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.IssueRestTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.github.jk1.ytplugin.rest.TimeTrackerRestClient
import com.github.jk1.ytplugin.rest.UserRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.*

class TimeTrackerTest : IssueRestTrait, IdeaProjectTrait, TaskManagerTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture
    private val issues = ArrayList<String>() //cleanup queue

    override lateinit var repository: YouTrackServer
    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        repository = createYouTrackRepository()
        repository.defaultSearch = "project: MT"
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
        Assert.assertEquals(0, storedIWorkItems.size)
    }

    @Test
    fun `test start timer functionality`() {
        issueStoreComponent[repository].update(repository).waitFor(5000)
        val timer = timeTrackerComponent
        Assert.assertEquals(timer.isRunning, false)
        val storedIssues = issueStoreComponent[repository].getAllIssues()
        val id = storedIssues[0].id
        timer.start(id)
        Assert.assertEquals(timer.isRunning, true)
    }

    @Test
    fun `test pause timer functionality`() {
        issueStoreComponent[repository].update(repository).waitFor(5000)
        val timer = timeTrackerComponent
        val storedIssues = issueStoreComponent[repository].getAllIssues()
        val id = storedIssues[0].id
        timer.start(id)
        Assert.assertEquals(timer.isRunning, true)
        Assert.assertEquals(timer.isPaused, false)
        timer.pause("Work timer paused")
        Assert.assertEquals(timer.isRunning, true)
        Assert.assertEquals(timer.isPaused, true)
    }

    @Test
    fun `test reset timer functionality`() {
        issueStoreComponent[repository].update(repository).waitFor(5000)
        val timer = timeTrackerComponent
        val storedIssues = issueStoreComponent[repository].getAllIssues()
        val id = storedIssues[0].id
        timer.start(id)
        Thread.sleep(60000)
        timer.reset()
        Assert.assertEquals(timer.isRunning, true)
        Assert.assertEquals(timer.isPaused, false)
        Assert.assertEquals(timer.recordedTime, "0")
    }


    // TODO better connect time tracking to MT project before run
    @Test
    fun `test post new work item`() {
        val storedIWorkItems = UserRestClient(repository).getWorkItemsForUser("")
        val size = storedIWorkItems.size

        if (storedIWorkItems.isNotEmpty()){
            val storedIssues = issueStoreComponent[repository].getAllIssues()
            val id = storedIssues[0].id
            TimeTrackerRestClient(repository).postNewWorkItem(id, "20","Testing",
                    "test item", (Date().time).toString())
            val storedIWorkItemsNew = issueWorkItemsStoreComponent[repository].getAllWorkItems()
            Assert.assertEquals(size + 1, storedIWorkItemsNew.size)
        } else {
            // time tracking is not connected, thus there are no work items
            Assert.assertEquals(size, 0)
        }
    }

    @After
    fun tearDown() {
        issueStoreComponent.remove(repository)
        issues.forEach { deleteIssue(it) }
        cleanUpTaskManager()
        fixture.tearDown()
    }
}