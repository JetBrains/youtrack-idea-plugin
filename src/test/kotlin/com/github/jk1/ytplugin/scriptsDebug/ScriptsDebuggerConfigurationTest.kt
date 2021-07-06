package com.github.jk1.ytplugin.scriptsDebug

import com.github.jk1.ytplugin.*
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.net.URI


class ScriptsDebuggerConfigurationTest : DebuggerRestTrait, IdeaProjectTrait, TaskManagerTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture

    override lateinit var repository: YouTrackServer
    override val project: Project by lazy { fixture.project }

    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        repository = createYouTrackRepository()
    }

//    @Test
//    fun `test getting debug ws url`() {
//        val address = InetSocketAddress(URI(repository.url).host, URI(repository.url).port)
//        val request = WipConnection().formJsonRequest(address, listOf(repository))
//        val ch = EmbeddedChannel(Http2StreamFrameToHttpObjectCodec(false))
//        ch.writeAndFlush(request).sync()
//        val info = WipConnection().getJsonInfo(request.decoderResult())
//    }

    @Test
    fun `test getting debug ws url without netty`() {
        val wsUrl = getWsUrl()
        logger.info("ws url: $wsUrl")
        assert(wsUrl != null)
    }


    @After
    fun tearDown() {
        issueStoreComponent.remove(repository)
        cleanUpTaskManager()
        fixture.tearDown()
    }
}