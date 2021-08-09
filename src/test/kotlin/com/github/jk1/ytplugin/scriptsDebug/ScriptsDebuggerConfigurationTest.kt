package com.github.jk1.ytplugin.scriptsDebug

import com.github.jk1.ytplugin.*
import com.github.jk1.ytplugin.rest.ScriptsRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.javascript.debugger.execution.RemoteUrlMappingBean
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.util.EventDispatcher
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled.directBuffer
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec
import org.jetbrains.debugger.DebugEventListener
import org.jetbrains.io.JsonReaderEx
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Callable
import kotlin.test.assertNotEquals


class ScriptsDebuggerConfigurationTest : DebuggerRestTrait, IdeaProjectTrait, SetupConnectionTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture

    override lateinit var repository: YouTrackServer
    override val project: Project by lazy { fixture.project }
    private val dispatcher: EventDispatcher<DebugEventListener> = EventDispatcher.create(DebugEventListener::class.java)
    private val debugEventListener: DebugEventListener get() = dispatcher.multicaster


    @Before
    fun setUp() {
        fixture = getLightCodeInsightFixture()
        fixture.setUp()
        repository = createYouTrackRepository(serverUrl, token)
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
        assertNotEquals(wsUrl, null)
    }

    @Test
    fun `test getting debug ws url with feature being turned off`() {
        val wsUrl: String? = getWsUrl("http://some-random-host:8080")
        assertEquals(wsUrl, null)
    }

    @Test
    fun `test getting debug ws url with not sufficient permissions`() {
        repository = createYouTrackRepository(serverUrl, "some-random-token")
        val wsUrl: String? = getWsUrl()
        assertEquals(wsUrl, null)
    }

    @Test
    fun `test scripts loading`() {
        val scriptsList = ScriptsRestClient(repository).getScriptsWithRules()
        assertEquals(scriptsList.size, 51)
    }


    @Test
    fun `test scripts placement in project`() {

        var srcDir: VirtualFile? = null
        ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
                srcDir = project.guessProjectDir()?.createChildDirectory(null, "src")
            }
        }

        ApplicationManager.getApplication().executeOnPooledThread(
            Callable {
                ScriptsRulesHandler(project).loadWorkflowRules(mutableListOf(), "src")
                assert(srcDir?.exists() == true && srcDir!!.children != null)
                val scriptsList = ScriptsRestClient(repository).getScriptsWithRules()

                // sublist is taken to reduce test execution time
                scriptsList.subList(1, 5).map { workflow ->
                    workflow.rules.map { rule ->
                        val existingScript = project.guessProjectDir()?.findFileByRelativePath(
                            "src/${workflow.name.split('/').last()}/${rule.name}.js"
                        )
                        assertNotEquals(existingScript, null)
                        //we can add more assertions on files names here
                    }
                }
            })
    }



    @Test
    fun `test getting address with the repository being not configured`() {

        val file = "src/test/resources/com/github/jk1/ytplugin/issues/debugger_endpoint_response.json"
        val json = Files.readAllBytes(Paths.get(file))

        val directBuffer: ByteBuf = directBuffer(256)

        val bytes = directBuffer.writeBytes(json)
        val wipConnection = WipConnection()
        wipConnection.getJsonInfo(bytes)

        //todo
//        assertEquals(wipConnection.getErrorNote(), "YouTrack server integration is not configured yet")
    }


    // id = 1 indicates that the artificial resume message was sent to debugger to reduce server load
    @Test(expected = IllegalArgumentException::class)
    fun `test artificial resume message`() {
        val message = " {\"result\":{},\"id\":1}"
        try {
            val channel = EmbeddedChannel(Http2StreamFrameToHttpObjectCodec(false))
            val vm = DebuggerWipVm(debugEventListener, getWsUrl(), channel)
            vm.commandProcessor.processIncomingJson(JsonReaderEx(message))
        } catch (e: IllegalArgumentException) {
            assertEquals("Cannot find callback with id 1", e.message)
            throw e
        }
    }


    @After
    fun tearDown() {
        issueStoreComponent.remove(repository)
        cleanUpTaskManager()
        val srcDir = project.guessProjectDir()?.findFileByRelativePath("src")

        ApplicationManager.getApplication().invokeAndWait {
            ApplicationManager.getApplication().runWriteAction {
                srcDir?.delete(null)
            }
        }

        fixture.tearDown()
    }
}