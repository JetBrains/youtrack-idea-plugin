package com.github.jk1.ytplugin.navigator

import com.github.jk1.ytplugin.IdeaProjectTrait
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.net.ServerSocket

class SourceNavigatorFallbackTest : IdeaProjectTrait {

    // heavy fixture delays component init until port layout is set by the test
    private val fixture: IdeaProjectTestFixture by lazy { getHeavyCodeInsightFixture() }

    private val firstFreePortFromRange: Int
        get() = (63330..63339).first {
            try {
                ServerSocket(it).close()
                true
            } catch(e: IOException) {
                false
            }
        }

    @Test
    fun testListenOnDefaultPort() {
        val expectedPort = firstFreePortFromRange
        fixture.setUp()
        val service = fixture.project.getService(SourceNavigatorService::class.java)

        Assert.assertNotNull(service)
        Assert.assertEquals(expectedPort, service.getActivePort())
    }

    @Test
    fun testFallbackIfDefaultPortIsOccupied() {
        val socket = ServerSocket(firstFreePortFromRange)
        val expectedPort = firstFreePortFromRange
        fixture.setUp()
        val service = fixture.project.getService(SourceNavigatorService::class.java)
        socket.close()

        Assert.assertNotNull(service)
        Assert.assertEquals(expectedPort, service.getActivePort())
    }

    @After
    fun tearDown() {
        fixture.tearDown()
    }

}
