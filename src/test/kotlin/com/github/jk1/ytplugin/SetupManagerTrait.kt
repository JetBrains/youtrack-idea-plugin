// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.github.jk1.ytplugin

import com.github.jk1.ytplugin.rest.RestClientTrait
import com.github.jk1.ytplugin.setupWindow.SetupTask
import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.UsernamePasswordCredentials
import org.apache.commons.httpclient.auth.AuthScope
import org.apache.commons.httpclient.methods.DeleteMethod
import org.apache.commons.httpclient.methods.PostMethod
import org.apache.commons.httpclient.methods.PutMethod

interface SetupManagerTrait : RestClientTrait, YouTrackConnectionTrait {

    override val httpClient: HttpClient
        get() {
            val client = HttpClient()
            client.params.connectionManagerTimeout = 30000 // ms
            client.params.soTimeout = 30000 // ms
            client.params.credentialCharset = "UTF-8"
            client.params.isAuthenticationPreemptive = true
            val credentials = UsernamePasswordCredentials(username, password)
            client.state.setCredentials(AuthScope.ANY, credentials)
            return client
        }
}