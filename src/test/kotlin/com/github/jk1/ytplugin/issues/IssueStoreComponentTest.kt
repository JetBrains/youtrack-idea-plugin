package com.github.jk1.ytplugin.issues

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

class IssueStoreComponentTest : IssueRestTrait, IdeaProjectTrait, TaskManagerTrait {

    lateinit var fixture: IdeaProjectTestFixture
    lateinit var server: YouTrackServer
    override val project: Project by lazy { fixture.project }
    val issues = ArrayList<String>() //cleanup queue

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        createIssue()
        server = createYouTrackRepository()
        server.defaultSearch = "project: AT"
    }

    override fun createIssue(summary: String): String {
        val issue = super.createIssue(summary)
        issues.add(issue)
        return issue
    }

    @Test
    fun testStoreLoad() {
        issueStoreComponent[server].update().waitFor(5000)

        val storedIssues = issueStoreComponent[server].getAllIssues()
        Assert.assertEquals(1, storedIssues.size)
        assertTrue(storedIssues.map { it.id }.contains(issues.first()))
    }

    @Test
    fun testCyrillicContentLoad() {
        // this test is windows-specific and depends on default platform encoding
        withDefaultCharset("windows-1251") {
            val expectedSummary = "Ехал грека через реку"
            createIssue(expectedSummary)
            issueStoreComponent[server].update().waitFor(5000)

            assertTrue(issueStoreComponent[server].getAllIssues().any {
                it.summary.equals(expectedSummary)
            })
        }
    }

    @Test
    fun testMultiProjectQuery() {
        server.defaultSearch = "project: AT, MT"

        issueStoreComponent[server].update().waitFor(5000)

        val storedIssues = issueStoreComponent[server].getAllIssues()
        assertTrue(storedIssues.size > 0)
        assertTrue(storedIssues.map { it.id }.contains(issues.first()))
    }

    @Test
    fun testImplicitSort() {
        val secondIssue = createIssue()
        val thirdIssue = createIssue()
        touchIssue(secondIssue)

        issueStoreComponent[server].update().waitFor(5000)

        with(issueStoreComponent[server]){
            Assert.assertEquals(3, getAllIssues().size)
            Assert.assertEquals(secondIssue, getIssue(0).id)
            Assert.assertEquals(thirdIssue, getIssue(1).id)
            Assert.assertEquals(issues.first(), getIssue(2).id)
        }
    }

    @Test
    fun testExplicitSort() {
        server.defaultSearch = "project: AT sort by: updated asc"
        val secondIssue = createIssue()
        val thirdIssue = createIssue()
        touchIssue(secondIssue)

        issueStoreComponent[server].update().waitFor(5000)

        with(issueStoreComponent[server]){
            Assert.assertEquals(3, getAllIssues().size)
            Assert.assertEquals(issues.first(), getIssue(0).id)
            Assert.assertEquals(thirdIssue, getIssue(1).id)
            Assert.assertEquals(secondIssue, getIssue(2).id)
        }
    }

    private fun withDefaultCharset(charset: String, code: () -> Unit) {
        // a hacky way to change 'file.encoding' property in runtime
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
        issues.forEach { deleteIssue(it) }
        cleanUpTaskManager()
        fixture.tearDown()
    }
}