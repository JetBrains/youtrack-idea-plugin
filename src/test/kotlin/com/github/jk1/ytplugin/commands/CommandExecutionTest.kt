package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.IssueRestTrait
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.tasks.Task
import com.intellij.tasks.TaskManager
import com.intellij.tasks.impl.TaskManagerImpl
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.tasks.youtrack.YouTrackRepositoryType
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.JavaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CommandExecutionTest : IssueRestTrait {

    lateinit var fixture: IdeaProjectTestFixture
    lateinit var taskManager: TaskManagerImpl
    lateinit var repository: YouTrackRepository
    lateinit var localTask: Task

    override val project: Project
        get() = fixture.project

    @Before
    fun setUp() {
        val ideaFactory = IdeaTestFixtureFactory.getFixtureFactory()
        val javaFactory = JavaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = ideaFactory.createLightFixtureBuilder(DefaultLightProjectDescriptor())
        fixture = javaFactory.createCodeInsightFixture(fixtureBuilder.fixture, LightTempDirTestFixtureImpl(true))
        fixture.setUp()

        repository = YouTrackRepository(YouTrackRepositoryType())
        repository.url = serverUrl
        repository.username = username
        repository.password = password
        repository.defaultSearch = ""

        taskManager = fixture.project.getComponent(TaskManager::class.java)!! as TaskManagerImpl
        taskManager.setRepositories(listOf(repository))
        localTask = repository.findTask(createIssue())!!

        readAction { taskManager.activateTask(localTask, true) }
    }

    @Test
    fun testCommandCompletion() {
        val command = YouTrackCommand("Fixed", 5, mutableListOf(localTask))
        val assist = commandComponent.suggest(command)

        Assert.assertNotNull(assist.suggestions.find { "Fixed".equals(it.option) })
        Assert.assertNotNull(assist.suggestions.find { "fixed in".equals(it.option) })
        Assert.assertNotNull(assist.suggestions.find { "Fixed in build".equals(it.option) })
    }

    @Test
    fun testCommandExecution() {
        val command = YouTrackCommand("Fixed", 5)
        val execution = YouTrackCommandExecution(command, false, null, "All Users")
        commandComponent.executeAsync(execution).get()

        Assert.assertTrue(repository.getIssues(localTask.id, 0, 1, true).first().isClosed)
    }

    @After
    fun tearDown() {
        deleteIssue(localTask.id)
        readAction {
            taskManager.localTasks.forEach { taskManager.removeTask(it) }
        }
        taskManager.setRepositories(listOf())
        fixture.tearDown()
    }

    private fun readAction(callback: () -> Unit) {
        val application = ApplicationManager.getApplication()
        application.invokeAndWait({
            application.runReadAction {
                callback.invoke()
            }
        }, application.anyModalityState)
    }

}