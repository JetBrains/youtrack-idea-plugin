package com.github.jk1.ytplugin.workflowsDebugConfiguration

import com.intellij.execution.RunConfigurationConverter
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy
import com.intellij.icons.AllIcons
import com.intellij.javascript.debugger.JSDebuggerBundle
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import icons.JavaScriptDebuggerIcons
import org.jdom.Element

private const val ID = "WorkflowsRemoteDebugType"
private const val FACTORY_ID = "Workflows Remote Debug"

class JSRemoteWorkflowsDebugConfigurationType
    : ConfigurationTypeBase(ID, "Remote Debug Of Workflows Scripts", null, NotNullLazyValue.createValue { AllIcons.Actions.IntentionBulbGrey }),
        DumbAware, RunConfigurationConverter {
    init {
        addFactory(object : ConfigurationFactory(this) {
            override fun getSingletonPolicy() = RunConfigurationSingletonPolicy.SINGLE_INSTANCE_ONLY

            override fun getId() = FACTORY_ID

            override fun createTemplateConfiguration(project: Project) = JSRemoteWorkflowsDebugConfiguration(project, this, configurationTypeDescription)

            override fun isEditableInDumbMode() = true
        })
    }

    override fun getTag() = "jsRemote"

    override fun convertRunConfigurationOnDemand(element: Element): Boolean {
        if (!isNodeJsRemoteDebugConfiguration(element)) {
            return false
        }

        element.setAttribute("type", ID)
        element.setAttribute("factoryName", FACTORY_ID)
        element.setAttribute("isV8Legacy", "true")
        val iterator = element.children.listIterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (next.name == "node-js-remote-debug") {
                next.getAttribute("host")?.let { element.setAttribute("host", it.value) }
                next.getAttribute("debug-port")?.let { element.setAttribute("port", it.value) }
                iterator.remove()
            }
        }
        return true
    }

    override fun getHelpTopic() = "reference.dialogs.rundebug.ChromiumRemoteDebugType"
}

private fun isNodeJsRemoteDebugConfiguration(element: Element): Boolean {
    return "NodeJSRemoteDebug".equals(element.getAttributeValue("type"), ignoreCase = true) &&
            // skip default to not disturb people
            "true" != element.getAttributeValue("default")
}
