// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.jk1.ytplugin.issues

import com.github.jk1.ytplugin.*
import com.github.jk1.ytplugin.setupWindow.NotifierState
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

class InputCredsTest : SetupManagerTrait, IdeaProjectTrait, SetupConnectionTrait, ComponentAware {

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
        val token = "perm:aWRlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"
        repository = createYouTrackRepository(serverUrl, token, false, false, false, false)
        val repo = repository.getRepo()
        val setupTask = SetupTask()
        setupTask.testConnection(repo, project)
        Assert.assertEquals("https://ytplugintest.myjetbrains.com/youtrack", setupTask.correctUrl)
        Assert.assertEquals(NotifierState.SUCCESS, setupTask.noteState)
        Assert.assertEquals(200, setupTask.statusCode)
    }

    @Test
    fun `test connection with ending error in url`() {
        val serverUrl = "https://ytplugintest.myjetbrains.com"
        val token = "perm:aWRlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"
        repository = createYouTrackRepository(serverUrl, token, false, false, false, false)
        val repo = repository.getRepo()
        val setupTask = SetupTask()
        setupTask.testConnection(repo, project)
        Assert.assertEquals("https://ytplugintest.myjetbrains.com/youtrack", setupTask.correctUrl)
        Assert.assertEquals(NotifierState.SUCCESS, setupTask.noteState)
        Assert.assertEquals(200, setupTask.statusCode)
    }

    @Test
    fun `test connection with HTTP and ending error in url`() {
        val serverUrl = "http://ytplugintest.myjetbrains.com"
        val token = "perm:aWRlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"
        repository = createYouTrackRepository(serverUrl, token, false, false, false, false)
        val repo = repository.getRepo()
        val setupTask = SetupTask()
        setupTask.testConnection(repo, project)
        Assert.assertEquals("https://ytplugintest.myjetbrains.com/youtrack", setupTask.correctUrl)
        Assert.assertEquals(NotifierState.SUCCESS, setupTask.noteState)
        Assert.assertEquals(200, setupTask.statusCode)
    }

    @Test
    fun `test connection with trailing slash error in url`() {
        val serverUrl = "https://ytplugintest.myjetbrains.com/youtrack/////"
        val token = "perm:aWRlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"
        repository = createYouTrackRepository(serverUrl, token, false, false, false, false)
        val repo = repository.getRepo()
        val setupTask = SetupTask()
        setupTask.testConnection(repo, project)
        Assert.assertEquals("https://ytplugintest.myjetbrains.com/youtrack", setupTask.correctUrl)
        Assert.assertEquals(NotifierState.SUCCESS, setupTask.noteState)
        Assert.assertEquals(200, setupTask.statusCode)
    }

    @Test
    fun `test connection with invalid token `() {
        val serverUrl = "https://ytplugintest.myjetbrains.com/youtrack/////"
        val token = "RlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"
        repository = createYouTrackRepository(serverUrl, token, false, false, false, false)
        val repo = repository.getRepo()
        val setupTask = SetupTask()
        setupTask.testConnection(repo, project)
        Assert.assertEquals(NotifierState.INVALID_TOKEN, setupTask.noteState)
        Assert.assertEquals(401, setupTask.statusCode)
    }

    @Test
    fun `test connection with non-existing url`() {
        val serverUrl = "lug"
        val token = "perm:aWRlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"
        repository = createYouTrackRepository(serverUrl, token, false, false, false, false)
        val repo = repository.getRepo()
        val setupTask = SetupTask()
        setupTask.testConnection(repo, project)
        Assert.assertEquals("https://lug/youtrack", setupTask.correctUrl)
        Assert.assertEquals(NotifierState.UNKNOWN_HOST, setupTask.noteState)
        Assert.assertEquals(401, setupTask.statusCode)
    }

    @Test
    fun `test connection with non-existing url looking like existing`() {
        val serverUrl = "https://tains.com"
        val token = "perm:aWRlcGx1Z2lu.NjItMA==.7iaoaBCduVgrbAj9BkQSxksQLQcEte"
        repository = createYouTrackRepository(serverUrl, token, false, false, false, false)
        val repo = repository.getRepo()
        val setupTask = SetupTask()
        setupTask.testConnection(repo, project)
        Assert.assertEquals("https://tains.com/youtrack", setupTask.correctUrl)
        Assert.assertEquals(NotifierState.LOGIN_ERROR, setupTask.noteState)
        Assert.assertEquals(401, setupTask.statusCode)
    }

    @Test
    fun `test connection with empty form`() {
        val serverUrl = ""
        val token = ""
        repository = createYouTrackRepository(serverUrl, token, false, false, false, false)
        val repo = repository.getRepo()
        val setupTask = SetupTask()
        setupTask.testConnection(repo, project)
        Assert.assertEquals("", setupTask.correctUrl)
        Assert.assertEquals(NotifierState.INVALID_TOKEN, setupTask.noteState)
        Assert.assertEquals(401, setupTask.statusCode)
    }

    @After
    fun tearDown() {
        issueStoreComponent.remove(repository)
        cleanUpTaskManager()
        fixture.tearDown()
    }
}
