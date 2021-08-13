package com.github.jk1.ytplugin.scriptsDebug

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.rest.AdminRestClient
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.google.common.collect.ImmutableBiMap
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configuration.EmptyRunProfileState
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.LocatableConfigurationBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction
import com.intellij.javascript.JSRunProfileWithCompileBeforeLaunchOption
import com.intellij.javascript.debugger.*
import com.intellij.javascript.debugger.execution.RemoteUrlMappingBean
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.SmartList
import com.intellij.util.proxy.ProtocolDefaultPorts
import com.intellij.util.ui.FormBuilder
import com.intellij.util.xmlb.SkipEmptySerializationFilter
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.debugger.wip.BrowserChromeDebugProcess
import org.jdom.Element
import org.jetbrains.debugger.DebuggableRunConfiguration
import org.jetbrains.io.LocalFileFinder
import java.net.InetSocketAddress
import java.net.URL
import javax.swing.JComponent
import javax.swing.JPanel


private const val DEFAULT_PORT = 443
private val SERIALIZATION_FILTER = SkipEmptySerializationFilter()

class JSRemoteScriptsDebugConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    LocatableConfigurationBase<Element>(project, factory, name),
    RunConfigurationWithSuppressedDefaultRunAction,
    JSRunProfileWithCompileBeforeLaunchOption,
    DebuggableRunConfiguration {
    @Attribute
    var host: String? = null

    @Attribute
    var port: Int = DEFAULT_PORT

    @Property(surroundWithTag = false)
    @XCollection
    var mappings: MutableList<RemoteUrlMappingBean> = SmartList()
        private set

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return WipRemoteDebugConfigurationSettingsEditor()
    }

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
        return EmptyRunProfileState.INSTANCE
    }

    override fun clone(): RunConfiguration {
        val configuration = super.clone() as JSRemoteScriptsDebugConfiguration
        configuration.host = host
        configuration.port = port
        configuration.mappings = SmartList(mappings)
        return configuration
    }

    @Throws(InvalidDataException::class)
    override fun readExternal(element: Element) {
        super<LocatableConfigurationBase>.readExternal(element)
        XmlSerializer.deserializeInto(this, element)
        if (port <= 0) {
            port = ProtocolDefaultPorts.SSL
        }
    }

    override fun writeExternal(element: Element) {
        super<LocatableConfigurationBase>.writeExternal(element)
        XmlSerializer.serializeInto(this, element, SERIALIZATION_FILTER)
    }

    override fun computeDebugAddress(state: RunProfileState): InetSocketAddress {
        if (port < 0) {
            port = 443
        }
        return InetSocketAddress(host, port)
    }


    private fun loadScripts() {
        val application = ApplicationManager.getApplication()
        application.invokeAndWait({
                ScriptsRulesHandler(project).loadWorkflowRules(mappings)
        }, application.noneModalityState)
    }

    override fun createDebugProcess(
        socketAddress: InetSocketAddress,
        session: XDebugSession,
        executionResult: ExecutionResult?,
        environment: ExecutionEnvironment
    ): XDebugProcess {
        return createWipDebugProcess(socketAddress, session, executionResult)
    }

    private fun createWipDebugProcess(
        socketAddress: InetSocketAddress,
        session: XDebugSession,
        executionResult: ExecutionResult?
    ): BrowserChromeDebugProcess {
        var process: BrowserChromeDebugProcess? = null

        val repo = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories()[0]
        val version = AdminRestClient(repo).getYouTrackVersion()

        // TODO: clear mappings on the run
        DumbService.getInstance(project).runReadActionInSmartMode() {

            when (version) {
                null -> throw InvalidDataException("The YouTrack Integration plugin has not been configured to connect with a YouTrack site")
                in 2021.3..Double.MAX_VALUE -> {

                    loadScripts()

                    val connection = WipConnection()

                    val finder = RemoteDebuggingFileFinder( createUrlToLocalMapping(mappings), LocalFileSystemFileFinder())

                    process = BrowserChromeDebugProcess(session, finder, connection, executionResult)
                    connection.open(socketAddress)

                    logger.info("connection is opened")

                    return@runReadActionInSmartMode
                }
                else -> throw InvalidDataException("YouTrack version is not sufficient")
            }
        }
        return process!!
    }


    private fun createUrlToLocalMapping(mappings: List<RemoteUrlMappingBean>): BiMap<String, VirtualFile> {
        if (mappings.isEmpty()) {
            return ImmutableBiMap.of()
        }

        val map = HashBiMap.create<String, VirtualFile>(mappings.size)
        for (mapping in mappings) {
            val file = LocalFileFinder.findFile(mapping.localFilePath)
            if (file != null) {
                map.forcePut(mapping.remoteUrl, file)
            }
        }
        return map
    }


    private inner class WipRemoteDebugConfigurationSettingsEditor :
        SettingsEditor<JSRemoteScriptsDebugConfiguration>() {

        override fun resetEditorFrom(configuration: JSRemoteScriptsDebugConfiguration) {}

        override fun applyEditorTo(configuration: JSRemoteScriptsDebugConfiguration) {
            val repositories = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories()
            if (repositories.isNotEmpty()) {
                logger.info("Apply Editor: $host, $port")
                configuration.host = URL(repositories[0].url).host
                configuration.port = URL(repositories[0].url).port
            }
        }

        override fun createEditor(): JComponent {
            val protocolPanel = JPanel(VerticalFlowLayout())
            return FormBuilder.createFormBuilder()
                .addComponent(protocolPanel, IdeBorderFactory.TITLED_BORDER_TOP_INSET)
                .panel
        }
    }

}
