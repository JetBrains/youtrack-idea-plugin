package com.github.jk1.ytplugin.search

import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.IssueRestTrait
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.*

class IssueStoreComponentTest : IssueRestTrait, IdeaProjectTrait {

    lateinit var fixture: IdeaProjectTestFixture
    override val project: Project by lazy { fixture.project }
    val issues = ArrayList<String>()

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
    }

    @Test
    fun testStoreLoad() {
        issueStoreComponent.update().waitFor(5000)
    }

    @After
    fun tearDown() {
        issues.forEach { deleteIssue(it) }
        fixture.tearDown()
    }
}