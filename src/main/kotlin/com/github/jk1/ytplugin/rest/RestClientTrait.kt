package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.tasks.YouTrackServer
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.util.net.IdeHttpClientHelpers
import com.intellij.util.net.ssl.CertificateManager
import org.apache.http.HttpRequest
import org.apache.http.HttpRequestInterceptor
import org.apache.http.HttpResponse
import org.apache.http.auth.AuthScope
import org.apache.http.auth.AuthScope.*
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.CredentialsProvider
import org.apache.http.client.HttpClient
import org.apache.http.client.config.AuthSchemes
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.conn.routing.HttpRoute
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.protocol.HttpContext
import org.apache.http.util.EntityUtils
import java.io.InputStreamReader
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*


interface RestClientTrait : ResponseLoggerTrait {

    val String.jsonEntity
        get() = StringEntity(this, ContentType.create(ContentType.APPLICATION_JSON.mimeType, StandardCharsets.UTF_8))

    val repository: YouTrackServer

    val httpClient: HttpClient
        get() {
            val requestConfigBuilder = RequestConfig.custom()
                    .setConnectTimeout(30000)  // ms
                    .setSocketTimeout(30000)   // ms
            if (repository.useProxy) {
                IdeHttpClientHelpers.ApacheHttpClient4.setProxyForUrlIfEnabled(requestConfigBuilder, repository.url)
            }
            val createCredentialsProvider: CredentialsProvider = BasicCredentialsProvider()
            // Basic authentication
            createCredentialsProvider.setCredentials(AuthScope(ANY_HOST, ANY_PORT, ANY_REALM, AuthSchemes.BASIC),
                    UsernamePasswordCredentials(repository.username, repository.password))
            // Proxy authentication
            if (repository.useProxy) {
                IdeHttpClientHelpers.ApacheHttpClient4.setProxyCredentialsForUrlIfEnabled(createCredentialsProvider, repository.url)
            }
            val builder: HttpClientBuilder = HttpClients.custom()
                    .setDefaultRequestConfig(requestConfigBuilder.build())
                    .setSSLContext(CertificateManager.getInstance().sslContext)
                    .setDefaultCredentialsProvider(createCredentialsProvider)
                    .addInterceptorFirst(PreemptiveBasicAuthInterceptor())
                    .addInterceptorLast { request: HttpRequest, _: HttpContext ->
                        request.setHeader("Accept", "application/json")
                        request.setHeader("User-Agent", "YouTrack IDE Plugin")
                    }
            return builder.build()
        }

    fun HttpUriRequest.execute(): Unit = execute { }

    fun <T> HttpUriRequest.execute(responseParser: (json: JsonElement) -> T): T {
        val response = httpClient.execute(this)
        try {
            if (response.statusLine.statusCode == 200) {
                val streamReader = InputStreamReader(response.responseBodyAsLoggedStream(), StandardCharsets.UTF_8.name())
                return responseParser.invoke(JsonParser.parseReader(streamReader))
            } else {
                throw RuntimeException(response.responseBodyAsLoggedString())
            }
        } finally {
            // closes the connection
            EntityUtils.consume(response.entity)
        }
    }

    val HttpResponse.responseBodyAsReader
        get() = InputStreamReader(entity.content, StandardCharsets.UTF_8)

    val String.urlencoded: String
        get() = URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private val String.b64Encoded: String
        get() = Base64.getEncoder().encodeToString(this.toByteArray(StandardCharsets.UTF_8))

    private class PreemptiveBasicAuthInterceptor : HttpRequestInterceptor {
        override fun process(request: HttpRequest, context: HttpContext) {
            val provider = context.getAttribute(HttpClientContext.CREDS_PROVIDER) as CredentialsProvider
            val credentials = provider.getCredentials(AuthScope(ANY_HOST, ANY_PORT, ANY_REALM, AuthSchemes.BASIC))
            if (credentials != null) {
                request.addHeader(BasicScheme(StandardCharsets.UTF_8).authenticate(credentials, request, context))
            }
            val proxyHost = (context.getAttribute(HttpClientContext.HTTP_ROUTE) as HttpRoute).proxyHost
            if (proxyHost != null) {
                val proxyCredentials = provider.getCredentials(AuthScope(proxyHost))
                if (proxyCredentials != null) {
                    request.addHeader(BasicScheme.authenticate(proxyCredentials, CharsetToolkit.UTF8, true))
                }
            }
        }
    }
}