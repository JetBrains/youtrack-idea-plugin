package com.github.jk1.ytplugin.scriptsDebug

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.debug.JSDebugScriptsEditor
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
import com.intellij.javascript.debugger.LocalFileSystemFileFinder
import com.intellij.javascript.debugger.execution.RemoteUrlMappingBean
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SmartList
import com.intellij.util.proxy.ProtocolDefaultPorts
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

class JSRemoteScriptsDebugConfiguration(project: Project, factory: ConfigurationFactory, name: String) :
    LocatableConfigurationBase<Element>(project, factory, name),
    RunConfigurationWithSuppressedDefaultRunAction,
    JSRunProfileWithCompileBeforeLaunchOption,
    DebuggableRunConfiguration {

    private val repositories = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories()
    private val repo = if (repositories.isNotEmpty()) {
        repositories.first()
    } else null

    private val DEFAULT_PORT = 443
    private val SERIALIZATION_FILTER = SkipEmptySerializationFilter()
    private val ROOT_FOLDER = "youtrack-scripts"
    private val INSTANCE_FOLDER = if (repo != null) URL(repo.url).host.split(".").first() else "Unnamed"

    @Attribute
    var host: String? = null

    @Attribute
    var port: Int = DEFAULT_PORT

    @Attribute
    var rootFolder: String = ROOT_FOLDER

    @Attribute
    var instanceFolder: String = INSTANCE_FOLDER

    @Property(surroundWithTag = false)
    @XCollection
    var mappings: MutableList<RemoteUrlMappingBean> = SmartList()
        private set

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return JSDebugScriptsEditor(project)
    }

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState? {
        return EmptyRunProfileState.INSTANCE
    }

    override fun clone(): RunConfiguration {
        val configuration = super.clone() as JSRemoteScriptsDebugConfiguration
        configuration.host = host
        configuration.port = port
        configuration.rootFolder = rootFolder
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

        val repositories = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories()
        val repo = if (repositories.isNotEmpty()) {
            repositories.first()
        } else null

        host = URL(repo?.url).host
        port = URL(repo?.url).port

        if (port < 0) {
            port = 443
        }

        logger.info("Connection is to be opened on $host:$port")
        return InetSocketAddress(host, port)
    }

    private fun loadScripts() {
        val application = ApplicationManager.getApplication()
        application.invokeAndWait({
                ScriptsRulesHandler(project).loadWorkflowRules(mappings, rootFolder, instanceFolder)
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

        val repositories = ComponentAware.of(project).taskManagerComponent.getAllConfiguredYouTrackRepositories()
        val repo = if (repositories.isNotEmpty()) repositories[0] else null
        val version = repo?.let { AdminRestClient(it).getYouTrackVersion() }

        // TODO: clear mappings on the run
            when (version) {
                null -> throw InvalidDataException("The YouTrack Integration plugin has not been configured to connect with a YouTrack site")
                in 2021.3..Double.MAX_VALUE -> {

                    // clear mappings on each run of the configuration
                    mappings.clear()

                    instanceFolder = if (repo != null) URL(repo.url).host.split(".").first() else "Unnamed"
                    loadScripts()

                    val connection = WipConnection()

                    val finder = RemoteDebuggingFileFinder( createUrlToLocalMapping(mappings), LocalFileSystemFileFinder(), rootFolder, instanceFolder)

                    process = BrowserChromeDebugProcess(session, finder, connection, executionResult)
                    connection.open(socketAddress)

                    logger.info("connection is opened")
                }
                else -> throw InvalidDataException("YouTrack version is not sufficient")
            }
        return process
    }


    private fun createUrlToLocalMapping(mappings: List<RemoteUrlMappingBean>): BiMap<String, VirtualFile> {
        if (mappings.isEmpty()) {
            return ImmutableBiMap.of()
        }

        val map = HashBiMap.create<String, VirtualFile>(mappings.size)
        for (mapping in mappings) {
            val file = LocalFileFinder.findFile(mapping.localFilePath)
            if (file != null) {
                logger.info("Url to local mapping: ${mapping.remoteUrl}, ${file.url}")
                map.forcePut(mapping.remoteUrl, file)
            }
        }
        return map
    }

}
