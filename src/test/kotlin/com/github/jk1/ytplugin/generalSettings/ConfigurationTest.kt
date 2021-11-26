package com.github.jk1.ytplugin.generalSettings

import com.github.jk1.ytplugin.IssueRestTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.github.jk1.ytplugin.setup.getInstanceUUID
import com.github.jk1.ytplugin.setup.getInstanceVersion
import com.github.jk1.ytplugin.setup.processUUID
import com.github.jk1.ytplugin.setup.processVersion
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonParser.parseString
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.BufferedReader
import java.io.File

class ConfigurationTest : IssueRestTrait, TaskManagerTrait {

    private lateinit var fixture: IdeaProjectTestFixture
    override val project: Project by lazy { fixture.project }
    override lateinit var repository: YouTrackServer

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        repository = createYouTrackRepository()

    }

    @Test
    fun `init configuration without version and uuid`(){

        val bufferedReader: BufferedReader = File("src/test/resources/com.github.jk1.ytplugin.general/config_no_version.json").bufferedReader()
        val inputString = bufferedReader.use { it.readText() }

        processVersion(parseString(inputString).asJsonObject)
        processUUID(parseString(inputString).asJsonObject)

        Assert.assertEquals(getInstanceVersion(), null)
        Assert.assertEquals(getInstanceUUID(), null)
    }

    @Test
    fun `init configuration with version and without uuid`(){

        val bufferedReader: BufferedReader = File("src/test/resources/com.github.jk1.ytplugin.general/config_no_uuid.json").bufferedReader()
        val inputString = bufferedReader.use { it.readText() }

        processVersion(parseString(inputString).asJsonObject)
        processUUID(parseString(inputString).asJsonObject)

        Assert.assertEquals(getInstanceVersion(), 2021.4)
        Assert.assertEquals(getInstanceUUID(), null)
    }

    @Test
    fun `init configuration with version and uuid`(){

        val bufferedReader: BufferedReader = File("src/test/resources/com.github.jk1.ytplugin.general/config_with_uuid.json").bufferedReader()
        val inputString = bufferedReader.use { it.readText() }

        processVersion(parseString(inputString).asJsonObject)
        processUUID(parseString(inputString).asJsonObject)

        Assert.assertEquals(getInstanceVersion(), 2021.4)
        Assert.assertEquals(getInstanceUUID(), "vdgdvf-2833772-dd")
    }

    @After
    fun tearDown() {
        cleanUpTaskManager()
        fixture.tearDown()
    }

}