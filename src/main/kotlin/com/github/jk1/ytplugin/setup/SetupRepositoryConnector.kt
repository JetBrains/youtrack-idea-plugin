package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.tasks.youtrack.YouTrackRepository
import com.intellij.util.net.IdeHttpClientHelpers
import com.intellij.util.net.ssl.CertificateManager
import io.netty.handler.codec.http.HttpScheme
import org.apache.http.HttpRequest
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.config.AuthSchemes
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import java.net.URL

class SetupRepositoryConnector {

    companion object {
        fun setupHttpClient(repository: YouTrackRepository): CloseableHttpClient {
            val requestConfigBuilder = RequestConfig.custom()
                .setConnectTimeout(60000)
                .setSocketTimeout(30000)


            if (repository.isUseProxy) {
                setupProxy(repository, requestConfigBuilder)
            }

            return HttpClientBuilder.create()
                .disableRedirectHandling()
                .setSSLContext(CertificateManager.getInstance().sslContext)
                .setDefaultRequestConfig(requestConfigBuilder.build()).build()
        }

        private fun setupProxy(repository: YouTrackRepository, requestConfigBuilder: RequestConfig.Builder ) {
            val createCredentialsProvider: CredentialsProvider = BasicCredentialsProvider()

            createCredentialsProvider.setCredentials(AuthScope(
                AuthScope.ANY_HOST,
                AuthScope.ANY_PORT,
                AuthScope.ANY_REALM, AuthSchemes.BASIC),
                UsernamePasswordCredentials(repository.username, repository.password))

            IdeHttpClientHelpers.ApacheHttpClient4
                .setProxyForUrlIfEnabled(requestConfigBuilder, repository.url)
            // Proxy authentication
            IdeHttpClientHelpers.ApacheHttpClient4
                .setProxyCredentialsForUrlIfEnabled(createCredentialsProvider, repository.url)
        }
    }

    @Volatile
    var noteState = NotifierState.INVALID_TOKEN
    private val endpoint = "/api/users/me?fields=name"

    private fun checkAndFixConnection(repository: YouTrackRepository, project: Project) {
        val checker = ConnectionChecker(repository, project)
        checker.onSuccess { request ->
            if (isValidYouTrackVersion(repository)) {
                repository.url = request.requestLine.uri.replace(endpoint, "")
                logger.debug("valid YouTrack version detected")
                noteState = NotifierState.SUCCESS
            } else {
                logger.debug("invalid YouTrack version detected")
                if (noteState != NotifierState.LOGIN_ERROR){
                    noteState = NotifierState.INVALID_VERSION
                }
            }
        }
        checker.onVersionError { _ ->

            if (getInstanceVersion() == null){
                obtainYouTrackConfiguration(repository)
            }
            val version = getInstanceVersion()

            if (version != null && version >= 2017.1 && version <= 2020.4) {
                logger.debug("valid YouTrack version detected but it is not sufficient for bearer token usage")
                noteState = NotifierState.INSUFFICIENT_FOR_TOKEN_VERSION
            } else {
                logger.debug("guest login is not allowed")
                if (noteState != NotifierState.LOGIN_ERROR){
                    noteState = NotifierState.INVALID_TOKEN
                }
            }
        }
        checker.onRedirectionError { request, response ->
            logger.debug("handling application error for ${repository.url}")
            when (response.statusLine.statusCode) {
                // handles both /youtrack absence (for old instances) and http instead of https protocol
                in 301..399 -> {
                    logger.debug("handling response code 301..399 for the ${repository.url}: REDIRECT")
                    val location = response.getFirstHeader("Location").value

                    replaceRepositoryUrlWithLocation(repository, location, request)

                    logger.debug("url after correction: ${repository.url}")
                    // unloaded instance redirect can't handle /api/* suffix properly
                    checker.check()
                }
                401, 403 -> {
                    logger.debug("handling response code 403 for the ${repository.url}: UNAUTHORIZED")
                    noteState = NotifierState.UNAUTHORIZED
                }
                else -> {
                    logger.debug("handling response code other than 301..399, 403 ${repository.url}: MANUAL FIX")
                    if (!request.requestLine.uri.contains("/youtrack")) {

                        repository.url = "${repository.url}/youtrack"
                        logger.debug("url after manual ending fix: ${repository.url}")

                        checker.check()
                    } else {
                        logger.debug("no manual ending fix: LOGIN_ERROR")
                        noteState = NotifierState.LOGIN_ERROR
                    }
                }
            }
        }
        checker.onTransportError { request: HttpRequest, _: Exception ->
            logger.debug("handling transport error for ${repository.url}")
            // handles https instead of http protocol
            if (URL(request.requestLine.uri).protocol == HttpScheme.HTTPS.toString()) {
                logger.debug("handling transport error for ${repository.url}: MANUAL PROTOCOL FIX")

                val repoUrl = URL(repository.url)
                repository.url = URL(HttpScheme.HTTP.toString(), repoUrl.host, repoUrl.port, repoUrl.path).toString()

                logger.debug("url after manual protocol fix: ${repository.url}")
                checker.check()
            } else {
                logger.debug("no manual transport fix: LOGIN_ERROR")
                noteState = NotifierState.LOGIN_ERROR
            }

        }
        checker.check()
    }

    private fun replaceRepositoryUrlWithLocation(repository: YouTrackRepository, location: String, request: HttpRequest) {
        if (!location.contains("/waitInstanceStartup/")) {
            repository.url =  if (location.contains(endpoint)){
                location.replace(endpoint, "")
            } else {
                "${repository.url}$location"
            }
        } else {
            if (!request.requestLine.uri.contains("/youtrack")) {
                logger.debug("url after manual ending fix for waitInstanceStartup : ${repository.url}")
                repository.url = "${repository.url}/youtrack"
            }
        }
    }


    fun testConnection(repository: YouTrackRepository, myProject: Project) {
        logger.debug("TRY CONNECTION FOR ${repository.url}")
        val task = object : Task.Modal(myProject, "Test connection", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Connecting to " + repository.url + "..."
                indicator.fraction = 0.0
                indicator.isIndeterminate = true

                checkAndFixConnection(repository, myProject)
            }
        }
        ProgressManager.getInstance().run(task)
    }


    private fun isValidYouTrackVersion(repo: YouTrackRepository): Boolean {
        // on the first invoke setup YouTrack configuration
        obtainYouTrackConfiguration(repo)
        val version = getInstanceVersion()
        return version != null && version >= 2017.1
    }
}