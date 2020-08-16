package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.editor.IssueLinkProviderExtension
import com.github.jk1.ytplugin.notifications.FetchNotificationsExtension
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
    fun `task management plugin is required and should be installed before YouTrack plugin`() {
        Assert.assertNotNull(TaskManager.getManager(fixture.project))
    }

    @Test
    fun `all plugin services should be accessible once the plugin is initialized`() {
        // fails on inconsistent plugin.xml, incomplete classpath and so on
        with (ComponentAware.of(fixture.project)){
            Assert.assertNotNull(taskManagerComponent)
            Assert.assertNotNull(commandComponent)
            Assert.assertNotNull(sourceNavigatorComponent)
            Assert.assertNotNull(issueStoreComponent)
            Assert.assertNotNull(issueUpdaterComponent)
            Assert.assertNotNull(pluginApiComponent)
        }
    }

    @Test
    fun `it should be possible to initialize all plugin extensions`() {
        FetchNotificationsExtension().runActivity(fixture.project)
        IssueLinkProviderExtension().runActivity(fixture.project)
    }

    @After
    fun tearDown() {
        fixture.tearDown()
    }
}