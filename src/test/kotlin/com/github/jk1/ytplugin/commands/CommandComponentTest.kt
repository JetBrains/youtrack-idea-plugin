package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.IssueRestTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.github.jk1.ytplugin.commands.components.CommandSession
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.common.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.tasks.Task
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CommandComponentTest : IssueRestTrait, IdeaProjectTrait, TaskManagerTrait {

    lateinit var fixture: IdeaProjectTestFixture
    lateinit var server: YouTrackServer
    lateinit var localTask: Task
    lateinit var session: CommandSession

    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        server = createYouTrackRepository()
        localTask = server.findTask(createIssue("summary"))!!
        readAction { getTaskManagerComponent().activateTask(localTask, true) }
        session = CommandSession(project)
    }

    @Test
    fun testCommandCompletion() {
        val command = YouTrackCommand(session, "Fixed", 5)
        val assist = commandComponent.suggest(command)

        Assert.assertNotNull(assist.suggestions.find { "Fixed".equals(it.option) })
        Assert.assertNotNull(assist.suggestions.find { "fixed in".equals(it.option) })
        Assert.assertNotNull(assist.suggestions.find { "Fixed in build".equals(it.option) })
    }

    @Test
    fun testCommandExecution() {
        val execution = YouTrackCommandExecution(session, "Fixed", false, null, "All Users")
        val future = commandComponent.executeAsync(execution)
        future.get() // wait for the command to complete

        Assert.assertTrue(server.getTasks(localTask.id, 0, 1).first().isClosed)
    }

    // 25-1268810 -> 4rnA
    // 25-76732 -> Iky

    @After
    fun tearDown() {
        deleteIssue(localTask.id)
        cleanUpTaskManager()
        fixture.tearDown()
    }
}