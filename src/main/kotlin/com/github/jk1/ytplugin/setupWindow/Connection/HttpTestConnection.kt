package com.github.jk1.ytplugin.setupWindow.Connection

import org.apache.commons.httpclient.HttpMethod

abstract class HttpTestConnection<T : HttpMethod?>(protected var myMethod: T) : CancellableConnection() {
    @Throws(Exception::class)
    override fun doTest() {
        doTest(myMethod)
    }

    override fun cancel() {
        myMethod!!.abort()
    }

    @Throws(Exception::class)
    protected abstract fun doTest(method: T)

}