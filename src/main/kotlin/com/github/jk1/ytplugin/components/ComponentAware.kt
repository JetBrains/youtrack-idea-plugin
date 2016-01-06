package com.github.jk1.ytplugin.components

import com.intellij.openapi.project.Project


interface ComponentAware {

    val project: Project

    val taskManagerComponent: TaskManagerProxyComponent
        get() = project.getComponent(TaskManagerProxyComponent::class.java)!!

    val commandComponent: CommandComponent
        get() = project.getComponent(CommandComponent::class.java)!!

    val restComponent: RestComponent
        get() = project.getComponent(RestComponent::class.java)!!
}