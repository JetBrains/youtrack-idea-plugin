package com.github.jk1.ytplugin.toolWindow

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Modal
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.tasks.TaskRepository
import com.intellij.tasks.TaskRepository.CancellableConnection
import com.intellij.util.containers.ContainerUtil
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


private abstract class TestConnectionTask internal constructor(title: String?, project: Project?) : Modal(project, title!!, true) {
    var myException: java.lang.Exception? = null
    protected var myConnection: CancellableConnection? = null
    override fun onCancel() {
        if (myConnection != null) {
            myConnection!!.cancel()
        }
    }
}

/**
 * Class for the task management
 */
class SetupTask (){

     fun testConnection(repository: TaskRepository, myProject: Project): Boolean {
         val myBadRepositories = ContainerUtil.newConcurrentSet<TaskRepository>()

         val task: TestConnectionTask = object : TestConnectionTask("Test connection", myProject) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Connecting to " + repository.url + "..."
                indicator.fraction = 0.0
                indicator.isIndeterminate = true
                try {
                    myConnection = repository.createCancellableConnection()
                    if (myConnection != null) {
                        val future = ApplicationManager.getApplication().executeOnPooledThread(myConnection!!)
                        while (true) {
                            try {
                                myException = future[100, TimeUnit.MILLISECONDS]
                                return
                            } catch (ignore: TimeoutException) {
                                try {
                                    indicator.checkCanceled()
                                } catch (e: ProcessCanceledException) {
                                    myException = e
                                    myConnection!!.cancel()
                                    return
                                }
                            } catch (e: Exception) {
                                myException = e
                                return
                            }
                        }
                    } else {
                        try {
                            repository.testConnection()
                        } catch (e: Exception) {
                            myException = e
                        }
                    }
                } catch (e: Exception) {
                    myException = e
                }
            }
        }
        ProgressManager.getInstance().run(task)
        val e = task.myException
        if (e == null) {
            myBadRepositories.remove(repository)
            Messages.showMessageDialog(myProject, "Connection is successful", "Connection", Messages.getInformationIcon())
        } else if (e !is ProcessCanceledException) {
            var message = e.message
            if (e is UnknownHostException) {
                message = "Unknown host: $message"
            }
            if (message == null) {
                message = "Unknown error"
            }
            Messages.showErrorDialog(myProject, StringUtil.capitalize(message), "Error")
        }
        return e == null
    }
}