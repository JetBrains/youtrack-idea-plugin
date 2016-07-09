package com.github.jk1.ytplugin.common.components

import com.github.jk1.ytplugin.commands.components.AdminComponent
import com.github.jk1.ytplugin.commands.components.CommandComponent
import com.github.jk1.ytplugin.navigator.components.SourceNavigatorComponent
import com.intellij.openapi.project.Project


interface ComponentAware {

    val project: Project

    companion object {
        fun of(aProject: Project): ComponentAware = object : ComponentAware {
            override val project: Project = aProject
        }
    }

    val taskManagerComponent: TaskManagerProxyComponent
        get() = project.getComponent(TaskManagerProxyComponent::class.java)!!

    val commandComponent: CommandComponent
        get() = project.getComponent(CommandComponent::class.java)!!

    val adminComponent: AdminComponent
        get() = project.getComponent(AdminComponent::class.java)!!

    val sourceNavigatorComponent: SourceNavigatorComponent
        get() = project.getComponent(SourceNavigatorComponent::class.java)!!
}