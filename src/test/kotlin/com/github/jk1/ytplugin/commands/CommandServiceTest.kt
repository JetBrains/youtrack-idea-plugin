package com.github.jk1.ytplugin.commands

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.IssueRestTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.github.jk1.ytplugin.commands.model.YouTrackCommand
import com.github.jk1.ytplugin.commands.model.YouTrackCommandExecution
import com.github.jk1.ytplugin.issues.model.Issue
import com.github.jk1.ytplugin.rest.IssuesRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.apache.commons.lang.StringEscapeUtils
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class CommandServiceTest : IssueRestTrait, IdeaProjectTrait, TaskManagerTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture
    private lateinit var issue: Issue

    override lateinit var repository: YouTrackServer
    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        repository = createYouTrackRepository()
        repository.defaultSearch = "project: AT"
        createIssue()
        issueStoreComponent[repository].update(repository).waitFor(5000)
        issue = issueStoreComponent[repository].getAllIssues().first()
    }

    @Test
    fun testCommandCompletion() {
        val command = YouTrackCommand(issue, "Fixed", 5)
        val assist = commandComponent.suggest(command)

        Assert.assertNotNull(assist.suggestions.find { "Fixed" == it.option })
        Assert.assertNotNull(assist.suggestions.find { "fixed in" == it.option })
        Assert.assertNotNull(assist.suggestions.find { "Fixed in build" == it.option })
    }

    @Test
    fun testCommandCompletionWithIssueInLocalStore() {
        issueStoreComponent[repository].update(repository).waitFor(5000)
        val command = YouTrackCommand(issue, "Fixed", 5)
        val assist = commandComponent.suggest(command)

        Assert.assertNotNull(assist.suggestions.find { "Fixed" == it.option })
        Assert.assertNotNull(assist.suggestions.find { "Fixed" == it.option })
        Assert.assertNotNull(assist.suggestions.find { "fixed in" == it.option })
        Assert.assertNotNull(assist.suggestions.find { "Fixed in build" == it.option })
    }

    @Test
    fun testCommandExecution() {
        val execution = YouTrackCommandExecution(issue, "Fixed", false, null, "All Users")
        val future = commandComponent.executeAsync(execution)
        future.get() // wait for the command to complete

        Assert.assertTrue(repository.getTasks(issue.id, 0, 1).first().isClosed)
    }

    @Test
    fun testCommandWithComment() {
        val text = "Look ma, a comment!"
        val execution = YouTrackCommandExecution(issue, "comment", false, text, "All Users")
        val future = commandComponent.executeAsync(execution)
        future.get() // wait for the command to complete

        val issueWithComment = IssuesRestClient(repository).getIssue(issue.id)
        Assert.assertEquals(1, issueWithComment.comments.size)
        Assert.assertEquals(text.wikiFormatted, issueWithComment.comments.first().text)
    }

    @Test
    fun testCommandWithMultilineCodeComment() {
        val text = """
            res.send({
                "err_code": 0,
                "data_list": []
            })
        """.trimIndent()
        val execution = YouTrackCommandExecution(issue, "comment", false, text, "All Users")
        val future = commandComponent.executeAsync(execution)
        future.get() // wait for the command to complete

        val issueWithComment = IssuesRestClient(repository).getIssue(issue.id)
        Assert.assertEquals(1, issueWithComment.comments.size)
        Assert.assertEquals(text.wikiFormatted, issueWithComment.comments.first().text)
    }

    @Test
    fun getVisibilityGroups() {
        commandComponent.getActiveTaskVisibilityGroups(issue) { groups ->
            Assert.assertEquals(3, groups.size)
            Assert.assertTrue(groups.contains("All Users"))
            Assert.assertTrue(groups.contains("Registered Users"))
            Assert.assertTrue(groups.contains("Automated Test-team"))
        }.get()
    }

    @After
    fun tearDown() {
        deleteIssue(issue.id)
        cleanUpTaskManager()
        fixture.tearDown()
    }

    private val String.wikiFormatted: String
        get() {
            val html = StringEscapeUtils
                .escapeHtml(this)
                .split("\n")
                .joinToString("<br/>") { it.trim() }
            return "<div class=\"wiki text common-markdown\"><p>$html</p>\n</div>"
        }
}