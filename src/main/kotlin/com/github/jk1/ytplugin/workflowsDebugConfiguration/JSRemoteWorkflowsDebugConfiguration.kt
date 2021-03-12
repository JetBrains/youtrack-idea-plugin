package com.github.jk1.ytplugin.workflowsDebugConfiguration

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
import com.intellij.javascript.debugger.execution.JSLocalFilesMappingPanel
import com.intellij.javascript.debugger.execution.RemoteUrlMappingBean
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.GuiUtils
import com.intellij.ui.HideableTitledPanel
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.PortField
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBRadioButton
import com.intellij.uiDesigner.core.AbstractLayout
import com.intellij.util.SmartList
import com.intellij.util.ui.FormBuilder
import com.intellij.util.xmlb.SkipEmptySerializationFilter
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.xdebugger.XDebugProcess
import com.intellij.xdebugger.XDebugSession
import com.jetbrains.debugger.wip.BrowserChromeDebugProcess
import org.apache.http.HttpHost
import org.apache.http.conn.HttpInetSocketAddress
import org.jdom.Element
import org.jetbrains.debugger.DebuggableRunConfiguration
import org.jetbrains.debugger.connection.VmConnection
import java.awt.BorderLayout
import java.net.InetAddress
import java.net.InetSocketAddress
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JPanel

private const val DEFAULT_PORT = 9229
private val SERIALIZATION_FILTER = SkipEmptySerializationFilter()

class JSRemoteWorkflowsDebugConfiguration(project: Project, factory: ConfigurationFactory, name: String)
    : LocatableConfigurationBase<Element>(project, factory, name),
        RunConfigurationWithSuppressedDefaultRunAction,
        JSRunProfileWithCompileBeforeLaunchOption,
        DebuggableRunConfiguration {
    @Attribute
    var host: String? = null
        set(value) {
            field = value
        }

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
        val configuration = super.clone() as JSRemoteWorkflowsDebugConfiguration
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
            port = DEFAULT_PORT
        }
    }

    override fun writeExternal(element: Element) {
        super<LocatableConfigurationBase>.writeExternal(element)

        XmlSerializer.serializeInto(this, element, SERIALIZATION_FILTER)
    }

    override fun computeDebugAddress(state: RunProfileState): InetSocketAddress {
        return host?.let {
            HttpInetSocketAddress(HttpHost(it), InetAddress.getLoopbackAddress(), port)
        } ?: HttpInetSocketAddress(null, InetAddress.getLoopbackAddress(), port)
    }

    override fun createDebugProcess(socketAddress: InetSocketAddress,
                                    session: XDebugSession,
                                    executionResult: ExecutionResult?,
                                    environment: ExecutionEnvironment): XDebugProcess {
        val debugProcess: JavaScriptDebugProcess<VmConnection<*>> =
                createWipDebugProcess(socketAddress, session, executionResult)
        return debugProcess
    }

    private fun createWipDebugProcess(socketAddress: InetSocketAddress,
                                      session: XDebugSession,
                                      executionResult: ExecutionResult?): BrowserChromeDebugProcess {
        val connection = WipConnection()
        val finder = RemoteDebuggingFileFinder(createUrlToLocalMap(mappings), LocalFileSystemFileFinder())
        // TODO process should be NodeChromeDebugProcess depending on PageConnection.type
        val process = BrowserChromeDebugProcess(session, finder, connection, executionResult)
        connection.open(socketAddress)
        return process
    }

    private inner class WipRemoteDebugConfigurationSettingsEditor : SettingsEditor<JSRemoteWorkflowsDebugConfiguration>() {
        private val hostField = GuiUtils.createUndoableTextField()

        private val portField = PortField(DEFAULT_PORT, 1024)
        private val wipRadioButton = JBRadioButton(JSDebuggerBundle.message("js.remote.debug.inspector.protocol"))
        private val filesMappingPanel: JSLocalFilesMappingPanel

        init {
            val buttonGroup = ButtonGroup()
            wipRadioButton.isSelected = true
            buttonGroup.add(wipRadioButton)
            filesMappingPanel = object : JSLocalFilesMappingPanel(project, BorderLayout()) {
                override fun initUI() {
                    add(mappingTreePanel)
                    super.initUI()
                }
            }
        }

        override fun resetEditorFrom(configuration: JSRemoteWorkflowsDebugConfiguration) {
            hostField.text = StringUtil.notNullize(configuration.host, "localhost")
            portField.number = configuration.port
            filesMappingPanel.resetEditorFrom(configuration.mappings, true)
        }

        override fun applyEditorTo(configuration: JSRemoteWorkflowsDebugConfiguration) {
            configuration.host = hostField.text
            configuration.port = portField.number
            filesMappingPanel.applyEditorTo(mappings, configuration)
        }

        override fun createEditor(): JComponent {
            val protocolPanel = JPanel(VerticalFlowLayout())
            protocolPanel.border = IdeBorderFactory.createTitledBorder(JSDebuggerBundle.message("js.remote.debug.attach.to"))
            protocolPanel.add(wipRadioButton)
            filesMappingPanel.initUI()
            val mappingsPanel = HideableTitledPanel(JSDebuggerBundle.message("label.text.remote.urls.of.local.files"), filesMappingPanel, true)
            return FormBuilder.createFormBuilder()
                    .addLabeledComponent(JSDebuggerBundle.message("js.remote.debug.host"), hostField)
                    .addLabeledComponent(JSDebuggerBundle.message("js.remote.debug.port"), portField)
                    .addComponent(protocolPanel, IdeBorderFactory.TITLED_BORDER_TOP_INSET)
                    .addComponentFillVertically(mappingsPanel, AbstractLayout.DEFAULT_VGAP * 2)
                    .panel
        }
    }

    companion object {
        var connectionToken: String? = null
    }

}
