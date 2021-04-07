package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.rest.RestClientTrait
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.config.SocketConfig
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.HttpClientBuilder

interface SetupManagerTrait : RestClientTrait, YouTrackConnectionTrait {

    override val httpClient: org.apache.http.client.HttpClient
        get() {
            val credentialsProvider = BasicCredentialsProvider()
            credentialsProvider.setCredentials(AuthScope.ANY, UsernamePasswordCredentials(username, password))
            val socketConfig = SocketConfig.custom().setSoTimeout(30000).build() // ms
            return HttpClientBuilder.create()
                    .setDefaultCredentialsProvider(credentialsProvider)
                    .setDefaultSocketConfig(socketConfig)
                    .build()
        }
}