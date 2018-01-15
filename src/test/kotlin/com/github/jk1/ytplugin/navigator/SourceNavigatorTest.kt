package com.github.jk1.ytplugin.navigator

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.TaskManagerTrait
import com.github.jk1.ytplugin.rest.RestClientTrait
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.http.client.utils.URLEncodedUtils
import org.apache.http.message.BasicNameValuePair
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import javax.imageio.ImageIO

class SourceNavigatorTest : RestClientTrait, IdeaProjectTrait, TaskManagerTrait, ComponentAware {

    private lateinit var fixture: JavaCodeInsightTestFixture
    private val ideaUrl: String by lazy { "http://127.0.0.1:${sourceNavigatorComponent.getActivePort()}" }

    override lateinit var repository: YouTrackServer
    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.testDataPath = "src/test/resources"
        fixture.setUp()
        repository = createYouTrackRepository()
        writeAction {
            fixture.configureByFile("testData/SourceNavigationTarget.kt")
        }
    }

    @Test
    fun testEditorNavigation() {
        val expectedLine = 3
        val expectedColumn = 5
        val params = listOf(
                BasicNameValuePair("file", "testData/SourceNavigationTarget.kt"),
                BasicNameValuePair("line", "${expectedLine + 1}"),
                BasicNameValuePair("offset", "$expectedColumn")
        )
        val method = GetMethod("$ideaUrl/file?${URLEncodedUtils.format(params, "utf-8")}")
        method.connect {
            HttpClient().executeMethod(method)
        }

        // assert the document document to be scrolled to requested position
        val caret = fixture.editor.caretModel.logicalPosition
        Assert.assertEquals(expectedLine, caret.line)
        Assert.assertEquals(expectedColumn, caret.column)
    }

    @Test
    fun testResponseToContainMarkerFile() {
        val params = listOf(
                BasicNameValuePair("file", "testData/SourceNavigationTarget.kt"),
                BasicNameValuePair("line", "3"),
                BasicNameValuePair("offset", "5")
        )
        val method = GetMethod("$ideaUrl/file?${URLEncodedUtils.format(params, "utf-8")}")
        method.connect {
            val responseStatus = HttpClient().executeMethod(method)
            val image = ImageIO.read(method.responseBodyAsStream)

            Assert.assertEquals(200, responseStatus)
            Assert.assertEquals(1, image.height)
            Assert.assertEquals(1, image.width)
        }
    }

    @Test
    fun testNavigateToMissingFile() {
        val params = listOf(
                BasicNameValuePair("file", "testData/Whatever.kt")
        )
        val method = GetMethod("$ideaUrl/file?${URLEncodedUtils.format(params, "utf-8")}")
        method.connect {
            HttpClient().executeMethod(method)
        }

        val caret = fixture.editor.caretModel.logicalPosition
        Assert.assertEquals(0, caret.line)
        Assert.assertEquals(0, caret.column)
    }

    @After
    fun tearDown() {
        fixture.tearDown()
    }
}