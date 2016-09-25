package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.ComponentAware
import com.intellij.tasks.TaskManager
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class PluginInstallationTest : IdeaProjectTrait {

    lateinit var fixture: IdeaProjectTestFixture

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
            Assert.assertNotNull(adminComponent)
            Assert.assertNotNull(commandComponent)
            Assert.assertNotNull(sourceNavigatorComponent)
            Assert.assertNotNull(taskManagerComponent)
            Assert.assertNotNull(issueStoreComponent)
        }
    }

    @After
    fun tearDown() {
        fixture.tearDown()
    }
}