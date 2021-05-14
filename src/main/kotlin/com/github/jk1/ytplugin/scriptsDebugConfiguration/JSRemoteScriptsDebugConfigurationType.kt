package com.github.jk1.ytplugin.scriptsDebugConfiguration

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy
import com.intellij.icons.AllIcons
import com.intellij.openapi.extensions.ExtensionNotApplicableException
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry

private const val ID = "WorkflowsRemoteDebugType"
private const val FACTORY_ID = "Workflows Remote Debug"

class JSRemoteScriptsDebugConfigurationType : ConfigurationTypeBase(
        ID,
        "Remote Debug Of Workflows Scripts",
        null,
        AllIcons.Actions.IntentionBulbGrey), DumbAware {

    init {
        if (Registry.`is`("youtrack.script.debug", false)) {
            addFactory(object : ConfigurationFactory(this) {
                override fun getSingletonPolicy() = RunConfigurationSingletonPolicy.SINGLE_INSTANCE_ONLY

                override fun getId() = FACTORY_ID

                override fun createTemplateConfiguration(project: Project) =
                        JSRemoteScriptsDebugConfiguration(project, this, configurationTypeDescription)

                override fun isEditableInDumbMode() = true
            })
        } else {
            throw ExtensionNotApplicableException.INSTANCE
        }
    }
}
