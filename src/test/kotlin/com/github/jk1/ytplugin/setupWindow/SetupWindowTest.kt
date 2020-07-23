package com.github.jk1.ytplugin.issues

import com.github.jk1.ytplugin.*
import com.github.jk1.ytplugin.setupWindow.SetupTask
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.jetbrains.rd.util.string.printToString
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.charset.Charset
import java.util.*
import javax.swing.JLabel

class SetupWindowTest : SetupManagerTrait, IdeaProjectTrait, SetupConnectionTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture
    override lateinit var repository: YouTrackServer
    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
    }

    @Test
    fun testHTTPErrorUrl() {
        val serverUrl = "http://ytplugintest.myjetbrains.com/youtrack"
        val token = "perm:aWRlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupTask()
        val notifier = JLabel()
        setupTask.testConnection(repo, project, notifier)
        Assert.assertEquals("https://ytplugintest.myjetbrains.com/youtrack", setupTask.correctUrl)
        Assert.assertEquals("Successfully connected", notifier.text)
        Assert.assertEquals(302, setupTask.statusCode)
    }

    // TODO: FIX ME
    @Test
    fun testEndingErrorUrl() {
        val serverUrl = "https://ytplugintest.myjetbrains.com"
        val token = "perm:aWRlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupTask()
        val notifier = JLabel()
        setupTask.testConnection(repo, project, notifier)
        Assert.assertEquals("https://ytplugintest.myjetbrains.com/youtrack", setupTask.correctUrl)
        Assert.assertEquals("Successfully connected", notifier.text)
        Assert.assertEquals(302, setupTask.statusCode)
    }

    @Test
    fun testTrSlashErrorUrl() {
        val serverUrl = "https://ytplugintest.myjetbrains.com/youtrack/////"
        val token = "perm:aWRlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupTask()
        val notifier = JLabel()
        setupTask.testConnection(repo, project, notifier)
        Assert.assertEquals("https://ytplugintest.myjetbrains.com/youtrack", setupTask.correctUrl)
        Assert.assertEquals("Successfully connected", notifier.text)
        Assert.assertEquals(200, setupTask.statusCode)
    }

    @Test
    fun testErrorToken() {
        val serverUrl = "https://ytplugintest.myjetbrains.com/youtrack/////"
        val token = "RlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupTask()
        val notifier = JLabel()
        setupTask.testConnection(repo, project, notifier)
        Assert.assertEquals("Cannot login: incorrect URL or token", notifier.text)
        Assert.assertEquals(401, setupTask.statusCode)
    }

    @Test
    fun testNonExisUrl() {
        val serverUrl = "lug"
        val token = "perm:aWRlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"
        repository = createYouTrackRepository(serverUrl, token)
        val repo = repository.getRepo()
        val setupTask = SetupTask()
        val notifier = JLabel()
        setupTask.testConnection(repo, project, notifier)
        Assert.assertEquals("https://lug", setupTask.correctUrl)
        Assert.assertEquals("Unknown host: lug", notifier.text)
        Assert.assertEquals(200, setupTask.statusCode)
    }

    @After
    fun tearDown() {
        issueStoreComponent.remove(repository)
        cleanUpTaskManager()
        fixture.tearDown()
    }
}