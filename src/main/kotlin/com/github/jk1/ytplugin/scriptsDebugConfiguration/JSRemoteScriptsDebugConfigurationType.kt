package com.github.jk1.ytplugin.scriptsDebugConfiguration

import com.intellij.execution.RunConfigurationConverter
import com.intellij.execution.configuration.ConfigurationFactoryEx
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.remoteServer.ServerType
import com.intellij.remoteServer.configuration.RemoteServersManager
import com.intellij.remoteServer.configuration.deployment.DeploymentConfigurator
import com.intellij.remoteServer.impl.configuration.deployment.DeployToServerRunConfiguration
import com.intellij.util.ArrayUtil
import org.jdom.Element
import org.jetbrains.annotations.NotNull

private const val ID = "WorkflowsRemoteDebugType"
private const val FACTORY_ID = "Workflows Remote Debug"

class JSRemoteScriptsDebugConfigurationType : ConfigurationTypeBase(
    ID,
    "Remote Debug Of Workflows Scripts",
    null,
    NotNullLazyValue.createValue { AllIcons.Actions.IntentionBulbGrey }), DumbAware, RunConfigurationConverter {

    init {
        addFactory(object : ConfigurationFactory(this) {
            override fun getSingletonPolicy() = RunConfigurationSingletonPolicy.SINGLE_INSTANCE_ONLY

            override fun getId() = FACTORY_ID

            override fun isApplicable(project: Project) = true

            override fun createTemplateConfiguration(project: Project) = JSRemoteScriptsDebugConfiguration(project, this, configurationTypeDescription)

            override fun isEditableInDumbMode() = true
        })
    }

    override fun getTag() = "jsRemote"

    override fun convertRunConfigurationOnDemand(element: Element): Boolean = false

    override fun getHelpTopic() = "reference.dialogs.rundebug.ChromiumRemoteDebugType"

     fun update(event: AnActionEvent) {
        event.presentation.isEnabledAndVisible = event.project != null
    }
}
