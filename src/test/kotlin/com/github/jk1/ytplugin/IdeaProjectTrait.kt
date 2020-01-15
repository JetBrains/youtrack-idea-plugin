package com.github.jk1.ytplugin

import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.*
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl
import org.apache.commons.lang.RandomStringUtils

interface IdeaProjectTrait {

    /**
     * Fast and reusable test project fixture, a default choice for a test
     */
    fun getLightCodeInsightFixture(): CodeInsightTestFixture {
        val ideaFactory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = ideaFactory.createLightFixtureBuilder(LightProjectDescriptor())
        return ideaFactory.createCodeInsightFixture(fixtureBuilder.fixture, LightTempDirTestFixtureImpl(true))
    }

    /**
     * Heavy fixture creates a new project every time, instantiating all project components
     */
    fun getHeavyCodeInsightFixture(): CodeInsightTestFixture {
        val ideaFactory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = ideaFactory.createFixtureBuilder(RandomStringUtils.randomAlphabetic(10))
        return ideaFactory.createCodeInsightFixture(fixtureBuilder.fixture, TempDirTestFixtureImpl())
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
        }, application.defaultModalityState)
    }
}