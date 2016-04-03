package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.commands.components.AdminComponent
import com.github.jk1.ytplugin.commands.components.CommandComponent
import com.github.jk1.ytplugin.navigator.components.SourceNavigatorComponent
import com.intellij.tasks.TaskManager
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class PluginInstallationTest {

    lateinit var fixture: IdeaProjectTestFixture

    @Before
    fun setUp() {
        val ideaFactory = IdeaTestFixtureFactory.getFixtureFactory()
        val javaFactory = JavaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = ideaFactory.createLightFixtureBuilder(DefaultLightProjectDescriptor())
        fixture = javaFactory.createCodeInsightFixture(fixtureBuilder.fixture, LightTempDirTestFixtureImpl(true))
        fixture.setUp()
    }

    @Test
    fun testTaskManagementPluginDependency() {
        Assert.assertNotNull(fixture.project?.getComponent(TaskManager::class.java))
    }

    @Test
    fun testPluginCanBeInstalled() {
        // fails on inconsistent plugin.xml, incomplete classpath and so on
        Assert.assertNotNull(fixture.project?.getComponent(AdminComponent::class.java))
        Assert.assertNotNull(fixture.project?.getComponent(CommandComponent::class.java))
        Assert.assertNotNull(fixture.project?.getComponent(SourceNavigatorComponent::class.java))
    }

    @After
    fun tearDown() {
        fixture.tearDown()
    }
}