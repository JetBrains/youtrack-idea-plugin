package com.github.jk1.ytplugin.issues

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.IssueRestTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.charset.Charset
import java.util.*

class IssueStoreComponentTestNewAPI : IssueRestTrait, IdeaProjectTrait, TaskManagerTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture
    private val issues = ArrayList<String>() //cleanup queue

    override lateinit var repository: YouTrackServer
    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        repository = createYouTrackRepository()
        repository.defaultSearch = "project: AT"
        createIssue()
    }

    override fun createIssue(summary: String): String {
        val issue = super.createIssue(summary)
        issues.add(issue)
        return issue
    }

    @Test
    fun testStoreLoad() {
        issueStoreComponent[repository].update(repository).waitFor(5000)

        val storedIssues = issueStoreComponent[repository].getAllIssues()
        Assert.assertEquals(1, storedIssues.size)
        assertTrue(storedIssues.map { it.id }.contains(issues.first()))
    }

    @Test
    fun testStoreUpdate() {
        issueStoreComponent[repository].update(repository).waitFor(5000)
        Assert.assertEquals(1, issueStoreComponent[repository].getAllIssues().size)
        repository.defaultSearch = "#Resolved"
        issueStoreComponent[repository].update(repository).waitFor(5000)
        Assert.assertEquals(0, issueStoreComponent[repository].getAllIssues().size)
    }

    @Test
    fun testCyrillicContentLoad() {
        // this test is windows-specific and depends on default platform encoding
        withDefaultCharset("windows-1251") {
            val expectedSummary = "Ехал грека через реку"
            createIssue(expectedSummary)
            issueStoreComponent[repository].update(repository).waitFor(5000)

            assertTrue(issueStoreComponent[repository].getAllIssues().any {
                it.summary == expectedSummary
            })
        }
    }

    @Test
    fun testMultiProjectQuery() {
        repository.defaultSearch = "project: AT, MT"

        issueStoreComponent[repository].update(repository).waitFor(5000)

        val storedIssues = issueStoreComponent[repository].getAllIssues()
        assertTrue(storedIssues.isNotEmpty())
        assertTrue(storedIssues.map { it.id }.contains(issues.first()))
    }

    @Test
    fun testImplicitSort() {
        val secondIssue = createIssue()
        val thirdIssue = createIssue()
        touchIssue(secondIssue)

        issueStoreComponent[repository].update(repository).waitFor(5000)

        with(issueStoreComponent[repository]){
            Assert.assertEquals(3, getAllIssues().size)
            Assert.assertEquals(secondIssue, getIssue(0).id)
            Assert.assertEquals(thirdIssue, getIssue(1).id)
            Assert.assertEquals(issues.first(), getIssue(2).id)
        }
    }

    @Test
    fun testExplicitSort() {
        repository.defaultSearch = "project: AT sort by: updated asc"
        val secondIssue = createIssue()
        val thirdIssue = createIssue()
        touchIssue(secondIssue)

        issueStoreComponent[repository].update(repository).waitFor(5000)

        with(issueStoreComponent[repository]){
            Assert.assertEquals(3, getAllIssues().size)
            Assert.assertEquals(issues.first(), getIssue(0).id)
            Assert.assertEquals(thirdIssue, getIssue(1).id)
            Assert.assertEquals(secondIssue, getIssue(2).id)
        }
    }

    private fun withDefaultCharset(charset: String, code: () -> Unit) {
        // a hacky way to change 'file.encoding' system property in runtime
        System.setProperty("file.encoding", charset)
        val charsetField = Charset::class.java.getDeclaredField("defaultCharset")
        charsetField.isAccessible = true
        val defaultCharset = charsetField.get(null)
        charsetField.set(null, null)
        try {
            code.invoke()
        } finally {
            // and revert everything afterwards
            System.setProperty("file.encoding", "UTF-8")
            charsetField.set(null, defaultCharset)
        }
    }

    @After
    fun tearDown() {
        issueStoreComponent.remove(repository)
        issues.forEach { deleteIssue(it) }
        cleanUpTaskManager()
        fixture.tearDown()
    }
}