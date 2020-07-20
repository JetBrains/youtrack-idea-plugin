package com.github.jk1.ytplugin.toolWindow

import com.github.jk1.ytplugin.toolWindow.Connection.CancellableConnection
import com.github.jk1.ytplugin.toolWindow.Connection.HttpTestConnection
import com.github.jk1.ytplugin.toolWindow.Connection.TestConnectionTask
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.io.HttpRequests
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.URI
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.PostMethod
import java.awt.Color
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.swing.JLabel


/**
 * Class for the task management
 */
class SetupTask() {


    @Tag("username")
    fun getRepositoryUsername(repository: YouTrackRepository): String? {
        return repository.username
    }

    @Tag("password")
    fun getRepositoryPassword(repository: YouTrackRepository): String? {
        return repository.password
    }

    private fun trimTrailingSlashes(url: String?): String? {
        if (url == null) return ""
        for (i in url.length - 1 downTo 0) {
            if (url[i] != '/') {
                return url.substring(0, i + 1)
            }
        }
        return ""
    }

    @Attribute("url")
    fun getRepositoryUrl(repository: YouTrackRepository): String? {
        return trimTrailingSlashes(repository.url)
    }

    @Throws(java.lang.Exception::class)
    private fun login(method: PostMethod, repository: YouTrackRepository): HttpClient? {
        val client: HttpClient = repository.httpClient
        client.state.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(getRepositoryUsername(repository), getRepositoryPassword(repository)))
        method.addParameter("login", getRepositoryUsername(repository))
        method.addParameter("password", getRepositoryPassword(repository))
//        method.followRedirects = true

        client.params.contentCharset = "UTF-8"
        client.executeMethod(method)
        var response: String?
        System.out.println("Code: " + method.statusCode + " Url: " + repository.url)
        response = try {
            if(method.statusCode > 300 && method.statusCode < 400){
                val location: String = method.getResponseHeader("Location").toString()
                val newUri = location.substring(10, location.length)
                System.out.println("New:" + newUri)
                method.uri = URI(newUri, false)
                System.out.println("method.uri" + method.uri)
//                client.executeMethod(method)
                repository.url = URI(newUri, false).toString()
                login(method, repository)
            }
            else if (method.statusCode == 403) {
                throw java.lang.Exception("Cannot login: token error occurred or user was banned")
            }
            else if (method.statusCode != 200) {
                throw HttpRequests.HttpStatusException("Cannot login", method.statusCode, method.path)
            }
            method.getResponseBodyAsString(1000)
        } finally {
            method.releaseConnection()
        }
        if (response == null) {
            throw NullPointerException()
        }
//        if (!response.contains("<login>ok</login>")) {
//            val pos = response.indexOf("</error>")
//            val length = "<error>".length
//            if (pos > length) {
//                response = response.substring(length, pos)
//            }
//            throw java.lang.Exception("Cannot login: $response")
//        }
        return client
    }

    fun createCancellableConnection(repository: YouTrackRepository): CancellableConnection? {
//        val method = PostMethod(getRepositoryUrl(repository) + "/rest/user/login")

        val method = PostMethod(getRepositoryUrl(repository) + "/api/token")
        method.setRequestHeader("Authorization","Bearer "+ repository.password)

        return object : HttpTestConnection<PostMethod?>(method) {
            @Throws(java.lang.Exception::class)
            override fun doTest(method: PostMethod?) {
                if (method != null) {
                    login(method, repository)
                }
            }
        }
    }


    fun testConnection(repository: YouTrackRepository, myProject: Project, notifier: JLabel): Boolean {
        val myBadRepositories = ContainerUtil.newConcurrentSet<YouTrackRepository>()

        val task: TestConnectionTask = object : TestConnectionTask("Test connection", myProject) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Connecting to " + repository.url + "..."
                indicator.fraction = 0.0
                indicator.isIndeterminate = true
                try {
                    myConnection = createCancellableConnection(repository)
                    if (myConnection != null) {
                        val future = ApplicationManager.getApplication().executeOnPooledThread(myConnection!!)
                        while (true) {
                            try {
                                myException = future[100, TimeUnit.MILLISECONDS]
                                notifier.text = "Cannot login"
                                return
                            } catch (ignore: TimeoutException) {
                                try {
                                    indicator.checkCanceled()
                                } catch (e: ProcessCanceledException) {
                                    myException = e
                                    notifier.text = "Error: " + e.message
                                    myConnection!!.cancel()
                                    return
                                }
                            } catch (e: Exception) {
                                myException = e
                                notifier.text = "Error: " + e.message

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
            notifier.foreground = Color.green;
            notifier.text = "Successfully connected"
//            Messages.showMessageDialog(myProject, "Connection is successful", "Connection", Messages.getInformationIcon())
        } else if (e !is ProcessCanceledException) {
            val message = e.message
            if (e is UnknownHostException) {
                notifier.text = "Unknown host: $message"
//                message = "Unknown host: $message"
            }
            if (message == null) {
                notifier.text = "Unknown error"
//                message = "Unknown error"
            }
//            Messages.showErrorDialog(myProject, StringUtil.capitalize(message), "Error")
        }
        return e == null
    }
}

