package com.github.jk1.ytplugin.navigator

import com.intellij.tasks.TaskManager
import com.intellij.testFramework.fixtures.*
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class SourceCodeNavigatorTest {

    lateinit var fixture: JavaCodeInsightTestFixture

    @Before
    fun setUp() {
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(DefaultLightProjectDescriptor())
        fixture = JavaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixtureBuilder.fixture, LightTempDirTestFixtureImpl(true))
        fixture.setUp()
        //fixture.testDataPath = getTestDataPath();

    }

    @Test
    fun testNavigateToExistingFile() {
        Assert.assertNotNull(fixture.project?.getComponent(TaskManager::class.java))
    }

    @Test
    fun testNavigateToMissingFile() {
        Assert.assertNotNull(fixture.project?.getComponent(TaskManager::class.java))
    }

    @After
    fun tearDown() {
        fixture.tearDown()
    }
}