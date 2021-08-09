package com.github.jk1.ytplugin.scriptsDebug

import com.github.jk1.ytplugin.ComponentAware
import com.github.jk1.ytplugin.debug.JSDebugScriptsEditor
import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.setup.SetupRepositoryConnector
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
import com.intellij.ide.browsers.WebBrowserXmlService
import com.intellij.ide.browsers.impl.WebBrowserServiceImpl
import com.intellij.javascript.JSRunProfileWithCompileBeforeLaunchOption
import com.intellij.javascript.debugger.LocalFileSystemFileFinder
import com.intellij.javascript.debugger.execution.RemoteUrlMappingBean
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.SmartList
import com.intellij.util.Url
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
import java.awt.GridLayout
import java.net.InetSocketAddress
import java.net.URL
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

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
    private val DEFAULT_FOLDER = "youtrack-scripts" + if (repo != null) "-${URL(repo.url).host}" else ""

    @Attribute
    var host: String? = null

    @Attribute
    var port: Int = DEFAULT_PORT

    @Attribute
    var folder: String = DEFAULT_FOLDER

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
        configuration.folder = folder
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
        element.setAttribute("uri", folder)
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
                ScriptsRulesHandler(project).loadWorkflowRules(mappings, folder)
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

        val version = repo?.let { SetupRepositoryConnector().getYouTrackVersion(it.url) }

        // TODO: clear mappings on the run
        DumbService.getInstance(project).runReadActionInSmartMode {

            when (version) {
                null -> throw InvalidDataException("The YouTrack Integration plugin has not been configured to connect with a YouTrack site")
                in 2021.3..Double.MAX_VALUE -> {

                    loadScripts()

                    val connection = WipConnection()

                    val finder = RemoteDebuggingFileFinder( createUrlToLocalMapping(mappings), LocalFileSystemFileFinder(), folder)

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

        private val folderField: TextFieldWithBrowseButton = TextFieldWithBrowseButton(JTextField("youtrack-scripts"))
        private val folderLabel: JLabel = JLabel("Download scripts to folder: ")

        override fun resetEditorFrom(configuration: JSRemoteScriptsDebugConfiguration) {
            val userFolder: String = configuration.folder
            folderField.text = userFolder
        }

        private fun virtualFileToUrl(file: VirtualFile, project: Project): Url? {
            val psiFile = ReadAction.compute<PsiFile?, RuntimeException> {
                PsiManager.getInstance(
                    project
                ).findFile(file)
            }
            return WebBrowserServiceImpl.getDebuggableUrl(psiFile)
        }


        override fun applyEditorTo(configuration: JSRemoteScriptsDebugConfiguration) {
            if (repo != null) {
                logger.info("Apply Editor: $host, $port")
                configuration.host = URL(repo.url).host
                configuration.port = URL(repo.url).port
                configuration.folder = folderField.text
            }
        }

        fun setupUrlField(field: TextFieldWithBrowseButton, project: Project) {
            val descriptor: FileChooserDescriptor =
                object : FileChooserDescriptor(true, false, false, false, false, false) {
                    override fun isFileSelectable(file: VirtualFile): Boolean {
                        return WebBrowserXmlService.getInstance()
                            .isHtmlFile(file) || virtualFileToUrl(file, project) != null
                    }
                }
            descriptor.description = "Select or create new folder to download youtrack scripts"
            descriptor.setRoots(*ProjectRootManager.getInstance(project).contentRoots)
            field.addBrowseFolderListener(object : TextBrowseFolderListener(descriptor, project) {
                override fun chosenFileToResultingText(chosenFile: VirtualFile): String {
                    if (chosenFile.isDirectory) {
                        return chosenFile.path
                    }
                    val url = virtualFileToUrl(chosenFile, project)
                    return if (url == null) chosenFile.url else url.toDecodedForm()
                }
            })
        }
        override fun createEditor(): JComponent {
            setupUrlField(folderField, project)
            val protocolPanel = JPanel(VerticalFlowLayout())
            val folderPanel = JPanel(GridLayout(1, 5))
            folderPanel.add(folderLabel)
            folderPanel.add(folderField)

            return FormBuilder.createFormBuilder()
                .addComponent(protocolPanel, IdeBorderFactory.TITLED_BORDER_TOP_INSET)
                .addComponent(folderPanel, IdeBorderFactory.TITLED_BORDER_TOP_INSET)
                .panel
        }
    }

}
