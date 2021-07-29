package com.github.jk1.ytplugin.scriptsDebug

import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons.YOUTRACK
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.extensions.ExtensionNotApplicableException
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.registry.Registry

private const val ID = "YouTrackRemoteDebugType"
private const val FACTORY_ID = "YouTrack Remote Debug"

class JSRemoteScriptsDebugConfigurationType : ConfigurationTypeBase(
    ID,
    "Remote Debug Of YouTrack Scripts",
    null,
    YOUTRACK
), DumbAware {

    init {
        val edition = ApplicationNamesInfo.getInstance().editionName
        logger.debug("IDE edition: $edition")

        if (Registry.`is`("youtrack.script.debug", false) && edition != "Community Edition") {
            addFactory(object : ConfigurationFactory(this) {
                override fun getSingletonPolicy() = RunConfigurationSingletonPolicy.SINGLE_INSTANCE_ONLY

                override fun getId() = FACTORY_ID

                override fun createTemplateConfiguration(project: Project) =
                    JSRemoteScriptsDebugConfiguration(project, this, configurationTypeDescription)

                override fun isEditableInDumbMode() = true

                override fun isApplicable(project: Project) = true
            })
        } else {
            throw ExtensionNotApplicableException.INSTANCE
        }
    }
}

