package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.commands.CommandComponent
import com.github.jk1.ytplugin.editor.AdminComponent
import com.github.jk1.ytplugin.issues.IssueStoreUpdaterComponent
import com.github.jk1.ytplugin.issues.PersistentIssueStoreComponent
import com.github.jk1.ytplugin.navigator.SourceNavigatorComponent
import com.github.jk1.ytplugin.notifications.NotificationsComponent
import com.github.jk1.ytplugin.tasks.TaskManagerProxyComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project


interface ComponentAware {

//    fun toggleIssueViewAction(project: Project, issueList: SetupList): ToggleIssueViewAction

    val project: Project

    companion object {

        fun of(aProject: Project): ComponentAware = object : ComponentAware {
            override val project: Project = aProject
        }

        fun <T> of(aProject: Project, closure: ComponentAware.() -> T): T
                = with(of(aProject)) { closure.invoke(this) }

    }

    val taskManagerComponent: TaskManagerProxyComponent
        get() = project.getComponent(TaskManagerProxyComponent::class.java)!!

    val commandComponent: CommandComponent
        get() = project.getComponent(CommandComponent::class.java)!!

    val adminComponent: AdminComponent
        get() = project.getComponent(AdminComponent::class.java)!!

    val sourceNavigatorComponent: SourceNavigatorComponent
        get() = project.getComponent(SourceNavigatorComponent::class.java)!!

    val issueStoreComponent: PersistentIssueStoreComponent
        get() = ApplicationManager.getApplication().getComponent(PersistentIssueStoreComponent::class.java)!!

    val issueUpdaterComponent: IssueStoreUpdaterComponent
        get() = project.getComponent(IssueStoreUpdaterComponent::class.java)!!

    val notificationsComponent: NotificationsComponent
        get() = project.getComponent(NotificationsComponent::class.java)!!

    val pluginApiComponent: YouTrackPluginApiComponent
        get() = project.getComponent(YouTrackPluginApi::class.java) as YouTrackPluginApiComponent
}