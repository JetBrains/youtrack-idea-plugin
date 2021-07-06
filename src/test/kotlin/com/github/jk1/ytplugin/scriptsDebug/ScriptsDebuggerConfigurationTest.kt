package com.github.jk1.ytplugin.scriptsDebug

import com.github.jk1.ytplugin.*
import com.github.jk1.ytplugin.rest.ScriptsRestClient
import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.IdeaProjectTestFixture
import com.intellij.util.EventDispatcher
import io.netty.channel.embedded.EmbeddedChannel
import io.netty.handler.codec.http2.Http2StreamFrameToHttpObjectCodec
import org.jetbrains.debugger.DebugEventListener
import org.jetbrains.io.JsonReaderEx
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.Callable
import kotlin.test.assertNotEquals


class ScriptsDebuggerConfigurationTest : DebuggerRestTrait, IdeaProjectTrait, TaskManagerTrait, ComponentAware {

    private lateinit var fixture: IdeaProjectTestFixture

    override lateinit var repository: YouTrackServer
    override val project: Project by lazy { fixture.project }
    private val dispatcher: EventDispatcher<DebugEventListener> = EventDispatcher.create(DebugEventListener::class.java)
    private val debugEventListener: DebugEventListener get() = dispatcher.multicaster


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
                ScriptsRulesHandler(project).loadWorkflowRules()
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