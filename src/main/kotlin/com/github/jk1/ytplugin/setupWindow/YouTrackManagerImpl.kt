package com.github.jk1.ytplugin.setupWindow
//
//import com.intellij.configurationStore.deserialize
//import com.intellij.openapi.Disposable
//import com.intellij.openapi.application.ApplicationManager
//import com.intellij.openapi.components.*
//import com.intellij.openapi.diagnostic.Logger
//import com.intellij.openapi.progress.EmptyProgressIndicator
//import com.intellij.openapi.progress.ProcessCanceledException
//import com.intellij.openapi.progress.ProgressIndicator
//import com.intellij.openapi.progress.ProgressManager
//import com.intellij.openapi.progress.Task.Backgroundable
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.project.ProjectManager
//import com.intellij.openapi.project.ProjectManagerListener
//import com.intellij.openapi.startup.StartupManager
//import com.intellij.openapi.ui.Messages
//import com.intellij.openapi.util.Comparing
//import com.intellij.openapi.util.text.StringUtil
//import com.intellij.openapi.vcs.AbstractVcs
//import com.intellij.openapi.vcs.ProjectLevelVcsManager
//import com.intellij.openapi.vcs.VcsTaskHandler
//import com.intellij.openapi.vcs.VcsType
//import com.intellij.openapi.vcs.changes.*
//import com.intellij.openapi.vcs.changes.shelf.ShelveChangesManager
//import com.intellij.serialization.SerializationException
//import com.intellij.tasks.LocalTask
//import com.intellij.tasks.TaskManager
//import com.intellij.tasks.youtrack.YouTrackRepository
//import com.intellij.ui.ColoredTreeCellRenderer
//import com.intellij.util.ArrayUtilRt
//import com.intellij.util.EventDispatcher
//import com.intellij.util.Function
//import com.intellij.util.containers.ContainerUtil
//import com.intellij.util.containers.Convertor
//import com.intellij.util.containers.MultiMap
//import com.intellij.util.io.HttpRequests
//import com.intellij.util.ui.TimerUtil
//import com.intellij.util.xmlb.XmlSerializerUtil
//import com.intellij.util.xmlb.annotations.Property
//import com.intellij.util.xmlb.annotations.Tag
//import com.intellij.util.xmlb.annotations.XCollection
//import org.jdom.Element
//import org.jetbrains.annotations.TestOnly
//import java.net.SocketTimeoutException
//import java.net.UnknownHostException
//import java.text.DecimalFormat
//import java.util.*
//import java.util.concurrent.TimeUnit
//import java.util.concurrent.TimeoutException
//import javax.swing.Timer
//
//@State(name = "YouTrackManager", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
//impl class YouTrackManagerImpl(private val myProject: Project) : TaskManager(), PersistentStateComponent<TaskManagerImpl.Config>, ChangeListDecorator, Disposable {
//
//
//    override fun getLocalTasks(): List<LocalTask> {
//        return getLocalTasks(true)
//    }
//
//
//    fun shelveChanges(task: LocalTask, shelfName: String) {
//        val changes = ChangeListManager.getInstance(myProject).defaultChangeList.changes
//        if (changes.isEmpty()) return
//        try {
//            ShelveChangesManager.getInstance(myProject).shelveChanges(changes, shelfName, true)
//            task.setShelfName(shelfName)
//        } catch (e: Exception) {
//        }
//    }
//
//    private fun unshelveChanges(task: LocalTask) {
//        val name = task.shelfName
//        if (name != null) {
//            val manager = ShelveChangesManager.getInstance(myProject)
//            val changeListManager = ChangeListManager.getInstance(myProject)
//            for (list in manager.shelvedChangeLists) {
//                if (name == list.DESCRIPTION) {
//                    manager.unshelveChangeList(list, null, list.binaryFiles, changeListManager.defaultChangeList, true)
//                    return
//                }
//            }
//        }
//    }
//
//    override fun dispose() {
//        if (myCacheRefreshTimer != null) {
//            myCacheRefreshTimer!!.stop()
//        }
//    }
//
//    override fun updateIssues(onComplete: Runnable?) {
//        val first = ContainerUtil.find(allRepositories) { repository: YouTrackRepository -> repository.isConfigured }
//        if (first == null) {
//            myIssueCache.clear()
//            onComplete?.run()
//            return
//        }
//        myUpdating = true
//        if (ApplicationManager.getApplication().isUnitTestMode) {
//            doUpdate(onComplete)
//        } else {
//            ApplicationManager.getApplication().executeOnPooledThread { doUpdate(onComplete) }
//        }
//    }
//
//    private fun doUpdate(onComplete: Runnable?) {
//        try {
//            val issues = getIssuesFromRepositories(null, 0, myConfig.updateIssuesCount, false, false, EmptyProgressIndicator())
//                    ?: return
//            synchronized(myIssueCache) {
//                myIssueCache.clear()
//                for (issue in issues) {
//                    myIssueCache[issue.id] = issue
//                }
//            }
//            // update local tasks
//            synchronized(myTasks) {
//                for ((key, value) in myTasks) {
//                    val issue = myIssueCache[key]
//                    if (issue != null) {
//                        value.updateFromIssue(issue)
//                    }
//                }
//            }
//        } finally {
//            onComplete?.run()
//            myUpdating = false
//        }
//    }
//
//}