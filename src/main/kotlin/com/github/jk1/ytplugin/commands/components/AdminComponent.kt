package com.github.jk1.ytplugin.commands.components

import com.github.jk1.ytplugin.common.components.ComponentAware
import com.intellij.openapi.components.ProjectComponent


interface AdminComponent : ProjectComponent, ComponentAware {

    fun getUserGroups(): List<String>
}