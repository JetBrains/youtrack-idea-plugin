package com.intellij.javascript.debugger.execution

import com.intellij.execution.ExecutionException
import com.intellij.execution.Executor
import com.intellij.execution.configuration.EmptyRunProfileState
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.ide.browsers.impl.WebBrowserServiceImpl
import com.intellij.javascript.debugger.JSDebuggerBundle
import com.intellij.javascript.debugger.execution.JavaScriptDebugRunner.Companion.findEngineAndBrowser
import com.intellij.javascript.debugger.execution.JavaScriptDebugSettingsEditor
import com.intellij.javascript.debugger.execution.RemoteUrlMappingBean
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiElement
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.listeners.UndoRefactoringElementAdapter
import com.intellij.util.PathUtil
import com.intellij.util.SmartList
import com.intellij.util.UriUtil
import com.intellij.util.Urls.newFromIdea
import com.intellij.util.io.URLUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Property
import com.intellij.util.xmlb.annotations.XCollection
import com.intellij.xml.util.HtmlUtil
import org.jdom.Element
import org.jetbrains.builtInWebServer.BuiltInServerOptions
import org.jetbrains.io.LocalFileFinder

class JavaScriptDebugConfiguration(project: Project, factory: ConfigurationFactory, name: String?) :
    LocatableConfigurationBase<Any?>(project, factory, name), RefactoringListenerProvider,
    RunProfileWithCompileBeforeLaunchOption {
//    private var uri: String? = null
    private var engineId: String? = null

    @get:Attribute
    var isUseFirstLineBreakpoints: Boolean = USE_FIRST_LINE_BREAKPOINTS_DEFAULT

    @get:Attribute
    var isUseBuiltInWebServerPort = false

    @Property(surroundWithTag = false)
    @XCollection
    private var mappings: MutableList<RemoteUrlMappingBean> = SmartList()
//    @Attribute
//    fun getUri(): String? {
//        if (isUseBuiltInWebServerPort && uri != null) {
//            val url = newFromIdea(uri!!)
//            val authority = url.authority
//            if (authority != null) {
//                val updatedAuthority = "localhost:" + BuiltInServerOptions.getInstance().effectiveBuiltInServerPort
//                return uri!!.replace(authority, updatedAuthority)
//            }
//        }
//        return uri
//    }

//    fun setUri(value: String?) {
//        uri = if (StringUtil.isEmptyOrSpaces(value)) null else value!!.trim { it <= ' ' }
//        isUseBuiltInWebServerPort = JavaScriptDebugConfiguration.Companion.usesBuiltInServerPort(uri)
//    }

    fun getMappings(): List<RemoteUrlMappingBean> {
        return mappings
    }

    fun setMappings(mappings: MutableList<RemoteUrlMappingBean>) {
        this.mappings = mappings
    }

//    /**
//     * In fact it returns a **browser id**
//     */
//    @Attribute
//    fun getEngineId(): String? {
//        return engineId
//    }
//
//    /**
//     * In fact you set a **browser id** with this setter
//     * @param browserId browser id
//     */
//    fun setEngineId(browserId: String?) {
//        engineId = StringUtil.nullize(browserId)
//    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return JavaScriptDebugSettingsEditor(project)
    }

    @Throws(RuntimeConfigurationException::class)
    override fun checkConfiguration() {
//        if (StringUtil.isEmptyOrSpaces(uri)) {
//            throw RuntimeConfigurationError(JSDebuggerBundle.message("javascript.debugger.url.is.not.specified"))
//        }
//        if (URLUtil.containsScheme(uri!!)) {
//            return
//        }
//        val file = LocalFileFinder.findFile(
//            VirtualFileManager.extractPath(
//                UriUtil.trimParameters(
//                    uri!!
//                )
//            )
//        )
//        if (file == null) {
//            throw RuntimeConfigurationError(JSDebuggerBundle.message("javascript.debugger.file.not.found"))
//        } else if (!HtmlUtil.isHtmlFile(file)) {
//            throw RuntimeConfigurationError(JSDebuggerBundle.message("javascript.debugger.file.not.html"))
//        }
    }

    override fun suggestedName(): String? {
//        return if (!StringUtil.isEmpty(uri)) {
//            PathUtil.getFileName(uri!!)
//        } else null
        return "kjj"
    }

    override fun clone(): RunConfiguration {
        val configuration = super.clone() as JavaScriptDebugConfiguration
//        configuration.uri = uri
//        configuration.engineId = engineId
        configuration.mappings = SmartList(mappings)
        return configuration
    }

    @Throws(ExecutionException::class)
    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState? {
//        if (StringUtil.isEmptyOrSpaces(uri)) {
//            throw ExecutionException(JSDebuggerBundle.message("javascript.debugger.url.is.not.specified"))
//        }
        return EmptyRunProfileState.INSTANCE
    }

    @Throws(InvalidDataException::class)
    override fun readExternal(element: Element) {
        super.readExternal(element)

        // we cannot use XmlSerializer - Attribute binding cannot handle null values correctly, "null" string will be, but must be null (as OptionTag does)
        val uri = element.getAttributeValue("uri")
//        setUri(uri)
        val updateWebServerPortValue = element.getAttributeValue("useBuiltInWebServerPort")
        if (updateWebServerPortValue != null) {
            isUseBuiltInWebServerPort = java.lang.Boolean.valueOf(updateWebServerPortValue)
        } else {
            isUseBuiltInWebServerPort = false
        }
//        engineId = StringUtil.nullize(element.getAttributeValue("engineId"))
        for (o in element.getChildren("mapping")) {
            val mapping = o as Element
            val remote = StringUtil.nullize(mapping.getAttributeValue("url"))
            val local = StringUtil.nullize(mapping.getAttributeValue("local-file"))
            if (local != null && remote != null) {
                mappings.add(RemoteUrlMappingBean(local, remote))
            }
        }
        val useFirstLineBreakpointsValue = element.getAttributeValue("useFirstLineBreakpoints")
        if (useFirstLineBreakpointsValue != null) {
            isUseFirstLineBreakpoints = java.lang.Boolean.valueOf(useFirstLineBreakpointsValue)
        }
    }

    @Throws(WriteExternalException::class)
    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        JavaScriptDebugConfiguration.Companion.serialize(this, element)
    }

    override fun isBuildBeforeLaunchAddedByDefault(): Boolean {
        return false
    }

    override fun getRefactoringElementListener(element: PsiElement): RefactoringElementListener? {
//        return if (uri == null) {
//            null
//        } else object : UndoRefactoringElementAdapter() {
//            override fun refactored(element: PsiElement, oldQualifiedName: String?) {
//                val url = WebBrowserServiceImpl.getDebuggableUrl(element)
//                if (url != null) {
//                    uri = url.toDecodedForm()
//                }
//            }
//        }
        return null
    }

    override fun toString(): String {
        return super.toString()
    }

    companion object {
        private const val USE_FIRST_LINE_BREAKPOINTS_DEFAULT = false
        private fun usesBuiltInServerPort(uri: String?): Boolean {
//            if (uri == null) return false
//            val url = uri?.let { newFromIdea(it) }
//            if (url?.authority == null) return false
//            val p = StringUtil.substringAfter(url.authority!!, "localhost:") ?: return false
//            try {
//                val port = p.toInt()
//                if (port == BuiltInServerOptions.getInstance().effectiveBuiltInServerPort) {
//                    return true
//                }
//            } catch (ignored: NumberFormatException) {
//            }
            return false
        }

        fun serialize(settings: JavaScriptDebugConfiguration, element: Element) {
//            if (!StringUtil.isEmpty(settings.engineId) &&
//                settings.engineId != findEngineAndBrowser(null).second.id.toString()
//            ) {
//                element.setAttribute("engineId", settings.engineId)
//            }
//            if (!StringUtil.isEmpty(settings.uri)) {
//                element.setAttribute("uri", settings.uri)
//            }
            for (mapping in settings.mappings) {
                if (!StringUtil.isEmpty(mapping.remoteUrl) && !StringUtil.isEmpty(mapping.localFilePath)) {
//                    val tag = Element("mapping")
//                    tag.setAttribute("url", mapping.remoteUrl)
//                    tag.setAttribute("local-file", mapping.localFilePath)
//                    element.addContent(tag)
                }
            }

        }
    }
}