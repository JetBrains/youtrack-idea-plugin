package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.commands.CommandService
import com.github.jk1.ytplugin.commands.ICommandService
import com.github.jk1.ytplugin.issues.IssueStoreUpdaterService
import com.github.jk1.ytplugin.issues.PersistentIssueStore
import com.github.jk1.ytplugin.navigator.SourceNavigatorService
import com.github.jk1.ytplugin.tasks.TaskManagerProxyService
import com.github.jk1.ytplugin.timeTracker.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project


interface ComponentAware {

    val project: Project

    companion object {

        fun of(aProject: Project): ComponentAware = object : ComponentAware {
            override val project: Project = aProject
        }

        fun <T> of(aProject: Project, closure: ComponentAware.() -> T): T
                = with(of(aProject)) { closure.invoke(this) }

    }

    val taskManagerComponent: TaskManagerProxyService
        get() = project.getService(TaskManagerProxyService::class.java)!!

    val commandComponent: ICommandService
        get() = project.getService(CommandService::class.java)!!

    val sourceNavigatorComponent: SourceNavigatorService
        get() = project.getService(SourceNavigatorService::class.java)!!

    val issueWorkItemsStoreComponent: PersistentIssueWorkItemsStore
        get() = ApplicationManager.getApplication().getService(PersistentIssueWorkItemsStore::class.java)!!

    val issueWorkItemsUpdaterComponent: IssueWorkItemsStoreUpdaterService
        get() = project.getService(IssueWorkItemsStoreUpdaterService::class.java)!!

    val issueStoreComponent: PersistentIssueStore
        get() = ApplicationManager.getApplication().getService(PersistentIssueStore::class.java)!!

    val issueUpdaterComponent: IssueStoreUpdaterService
        get() = project.getService(IssueStoreUpdaterService::class.java)!!

    val pluginApiComponent: YouTrackPluginApiService
        get() = project.getService(YouTrackPluginApiService::class.java) as YouTrackPluginApiService

    val timeTrackerStoreComponent: PersistentTimeTrackerStore
        get() = ApplicationManager.getApplication().getService(PersistentTimeTrackerStore::class.java)!!

    val timeTrackerUpdaterComponent: TimeTrackerStoreUpdaterService
        get() = project.getService(TimeTrackerStoreUpdaterService::class.java)!!

    val timeTrackerComponent: TimeTracker
        get() = project.getService(TimeTracker::class.java)!!

}