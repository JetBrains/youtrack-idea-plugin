package com.github.jk1.ytplugin.components

import com.intellij.openapi.components.ProjectComponent


interface AdminComponent : ProjectComponent, ComponentAware {

    fun getUserGroups(): List<String>
}