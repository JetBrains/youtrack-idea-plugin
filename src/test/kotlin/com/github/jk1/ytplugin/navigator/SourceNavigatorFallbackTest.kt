package com.github.jk1.ytplugin.navigator

import com.github.jk1.ytplugin.IdeaProjectTrait
import com.github.jk1.ytplugin.navigator.SourceNavigatorComponent
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Assert
import org.junit.Test
import java.io.IOException
import java.net.ServerSocket

class SourceNavigatorFallbackTest : IdeaProjectTrait {

    // heavy fixture delays component init until port layout is set by the test
    val fixture: IdeaProjectTestFixture by lazy { getHeavyCodeInsightFixture() }

    val firstFreePortFromRange: Int
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
        val component = fixture.project.getComponent(SourceNavigatorComponent::class.java)

        Assert.assertEquals(expectedPort, component?.getActivePort())
    }

    @Test
    fun testFallbackIfDefaultPortIsOccupied() {
        val socket = ServerSocket(firstFreePortFromRange)
        val expectedPort = firstFreePortFromRange
        fixture.setUp()
        val component = fixture.project.getComponent(SourceNavigatorComponent::class.java)
        socket.close()

        Assert.assertEquals(expectedPort, component?.getActivePort())
    }

    @After
    fun tearDown() {
        fixture.tearDown()
    }

}
