package com.github.jk1.ytplugin.setupWindow

import com.github.jk1.ytplugin.setupWindow.Connection.CancellableConnection
import com.github.jk1.ytplugin.setupWindow.Connection.HttpTestConnection
import com.github.jk1.ytplugin.setupWindow.Connection.TestConnectionTask
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
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

    var correctUrl:String = ""

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

    fun isValidToken(token: String) : Boolean{
        val tokenPattern = Regex("perm:([^.]+)\\.([^.]+)\\.(.+)")
        return token.matches(tokenPattern)
    }

    private fun fixURI(method: PostMethod){
        if (!method.uri.toString().contains("https")){
            val newUri = "https" + method.uri.toString().substring(4, method.uri.toString().length)
            method.uri = URI(newUri, false)
            System.out.println("new url:" + method.uri)
        }
        if(!method.uri.toString().contains("com/youtrack") && correctUrl.contains("com/youtrack")){
            val newUri = method.uri.toString() + "/youtrack"
            method.uri = URI(newUri, false)
        }
        else{
            throw HttpRequests.HttpStatusException("Cannot login: incorrect URL or token", method.statusCode, method.path)
        }
    }

    @Throws(java.lang.Exception::class)
    private fun login(method: PostMethod, repository: YouTrackRepository): HttpClient? {
        val client: HttpClient = repository.httpClient
        client.state.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(getRepositoryUsername(repository), getRepositoryPassword(repository)))
        method.addParameter("login", getRepositoryUsername(repository))
        method.addParameter("password", getRepositoryPassword(repository))

        client.params.contentCharset = "UTF-8"
        client.executeMethod(method)
        val response: String?
        System.out.println("Code: " + method.statusCode + " Url: " + repository.url)
        response = try {
            if(method.statusCode > 300 && method.statusCode < 400){
                val location: String = method.getResponseHeader("Location").toString()
                val newUri = location.substring(10, location.length)
                method.uri = URI(newUri, false)
                /* substring(0,  newUri.length - 12) is needed to get rid of "/api/token" ending */
                repository.url = method.uri.toString().substring(0,  newUri.length - 12)
                correctUrl = repository.url
                createCancellableConnection(repository)
            }
            else if (method.statusCode == 403) {
                throw java.lang.Exception("Cannot login: token error occurred or user was banned")
            }
            else if (method.statusCode != 200) {
                fixURI(method)
                repository.url = method.uri.toString().substring(0, method.uri.toString().length - 12)
                createCancellableConnection(repository)
            }
            method.getResponseBodyAsString(1000)
        } finally {
            method.releaseConnection()
        }
        if (response == null) {
            throw NullPointerException()
        }
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
                correctUrl = repository.url
                try {
                    myConnection = createCancellableConnection(repository)
                    if (myConnection != null) {
                        val future = ApplicationManager.getApplication().executeOnPooledThread(myConnection!!)
                        while (true) {
                            try {
                                myException = future[100, TimeUnit.MILLISECONDS]
                                notifier.text = "Cannot login: incorrect URL or token"
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
            correctUrl = repository.url
            notifier.foreground = Color.green;
            notifier.text = "Successfully connected"
        } else if (e !is ProcessCanceledException) {
            val message = e.message
            if (e is UnknownHostException) {
                notifier.text = "Unknown host: $message"
            }
            if (message == null) {
                notifier.text = "Unknown error"
            }
        }
        return e == null
    }
}

