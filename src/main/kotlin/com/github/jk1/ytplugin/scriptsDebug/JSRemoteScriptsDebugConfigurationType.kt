package com.github.jk1.ytplugin.scriptsDebug

import com.github.jk1.ytplugin.logger
import com.github.jk1.ytplugin.ui.YouTrackPluginIcons.YOUTRACK
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.extensions.ExtensionNotApplicableException
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project

private const val ID = "YouTrackRemoteDebugType"
private const val FACTORY_ID = "YouTrack Remote Debug"

class JSRemoteScriptsDebugConfigurationType : ConfigurationTypeBase(
    ID,
    "YouTrack Debug",
    null,
    YOUTRACK
), DumbAware {

    init {
        val edition = ApplicationNamesInfo.getInstance().editionName
        logger.debug("IDE edition: $edition")

        val actionManager = ActionManager.getInstance()
        val areJsAndJSDebuggerPluginsEnabled = actionManager.getActionIdList("JavaScript").isNotEmpty() &&
                actionManager.getActionIdList("JavaScriptDebugger").isNotEmpty()

        if (edition != "Community Edition" && areJsAndJSDebuggerPluginsEnabled) {
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

