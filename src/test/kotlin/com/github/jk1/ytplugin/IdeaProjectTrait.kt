package com.github.jk1.ytplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleType
import com.intellij.testFramework.fixtures.*
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import org.apache.commons.lang.RandomStringUtils

interface IdeaProjectTrait {

    /**
     * Fast and reusable test project fixture, a default choice for a test
     */
    fun getLightCodeInsightFixture(): JavaCodeInsightTestFixture {
        val ideaFactory = IdeaTestFixtureFactory.getFixtureFactory()
        val javaFactory = JavaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = ideaFactory.createLightFixtureBuilder(DefaultLightProjectDescriptor())
        return javaFactory.createCodeInsightFixture(fixtureBuilder.fixture, LightTempDirTestFixtureImpl(true))
    }

    /**
     * Heavy fixture creates a new project every time, instantiating all project components
     */
    fun getHeavyCodeInsightFixture(): JavaCodeInsightTestFixture {
        val ideaFactory = IdeaTestFixtureFactory.getFixtureFactory()
        val javaFactory = JavaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = ideaFactory.createFixtureBuilder(RandomStringUtils.randomAlphabetic(10))
        return javaFactory.createCodeInsightFixture(fixtureBuilder.fixture, LightTempDirTestFixtureImpl(true))
    }

    fun readAction(callback: () -> Unit) {
        val application = ApplicationManager.getApplication()
        application.invokeAndWait({
            application.runReadAction {
                callback.invoke()
            }
        }, application.anyModalityState)
    }

    fun writeAction(callback: () -> Unit) {
        val application = ApplicationManager.getApplication()
        application.invokeAndWait({
            application.runWriteAction {
                callback.invoke()
            }
        }, application.anyModalityState)
    }
}