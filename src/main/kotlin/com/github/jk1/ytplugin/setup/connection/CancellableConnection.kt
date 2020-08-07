package com.github.jk1.ytplugin.setup.connection

import java.util.concurrent.Callable

abstract class CancellableConnection : Callable<Exception?> {
    override fun call(): Exception? {
        return try {
            doTest()
            null
        } catch (e: Exception) {
            e
        }
    }

    @Throws(Exception::class)
    protected abstract fun doTest()
    abstract fun cancel()
}