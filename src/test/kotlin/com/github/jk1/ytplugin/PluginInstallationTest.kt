package com.github.jk1.ytplugin

import com.intellij.tasks.TaskManager
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class PluginInstallationTest : IdeaProjectTrait {

    private lateinit var fixture: IdeaProjectTestFixture

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
    }

    @Test
    fun testTaskManagementPluginDependency() {
        Assert.assertNotNull(fixture.project?.getComponent(TaskManager::class.java))
    }

    @Test
    fun testPluginCanBeInstalled() {
        // fails on inconsistent plugin.xml, incomplete classpath and so on
        with (ComponentAware.of(fixture.project)){
            Assert.assertNotNull(taskManagerComponent)
            Assert.assertNotNull(commandComponent)
            Assert.assertNotNull(adminComponent)
            Assert.assertNotNull(sourceNavigatorComponent)
            Assert.assertNotNull(issueStoreComponent)
            Assert.assertNotNull(issueUpdaterComponent)
            Assert.assertNotNull(notificationsComponent)
            Assert.assertNotNull(pluginApiComponent)
        }
    }

    @After
    fun tearDown() {
        fixture.tearDown()
    }
}