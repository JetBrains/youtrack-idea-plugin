package com.github.jk1.ytplugin.issues

import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.IssueRestTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.nio.charset.Charset
import java.util.*

class IssueStoreComponentTest : IssueRestTrait, IdeaProjectTrait, TaskManagerTrait {

    lateinit var fixture: IdeaProjectTestFixture
    lateinit var server: YouTrackServer
    override val project: Project by lazy { fixture.project }
    val issues = ArrayList<String>()

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        issues.add(createIssue())
        server = createYouTrackRepository()
        server.defaultSearch = "project: AT"
    }

    @Test
    fun testStoreLoad() {
        issueStoreComponent[server].update().waitFor(5000)

        val storedIssues = issueStoreComponent[server].getAllIssues()
        Assert.assertEquals(1, storedIssues.size)
        Assert.assertTrue(storedIssues.map { it.id }.contains(issues.first()))
    }

    @Test
    fun testCyrillicContentLoad() {
        // this test is windows-specific and depends on default platform encoding
        withDefaultCharset("windows-1251") {
            val expectedSummary = "Ехал грека через реку"
            val issue = createIssue(expectedSummary)
            issues.add(issue)
            issueStoreComponent[server].update().waitFor(5000)

            Assert.assertTrue(issueStoreComponent[server].getAllIssues().any {
                it.summary.equals(expectedSummary)
            })
        }
    }

    @Test
    fun testMultiProjectQuery() {
        server.defaultSearch = "project: AT, MT"

        issueStoreComponent[server].update().waitFor(5000)

        val storedIssues = issueStoreComponent[server].getAllIssues()
        Assert.assertTrue(storedIssues.size > 0)
        Assert.assertTrue(storedIssues.map { it.id }.contains(issues.first()))
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