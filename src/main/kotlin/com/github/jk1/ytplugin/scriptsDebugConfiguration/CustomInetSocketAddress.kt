package com.github.jk1.ytplugin.scriptsDebugConfiguration

import org.apache.http.HttpHost
import org.apache.http.util.Args
import java.net.InetAddress
import java.net.InetSocketAddress

class CustomInetSocketAddress(private val httphost: HttpHost?, addr: InetAddress?, port: Int) :
    InetSocketAddress(addr, port) {

    private val DEFAULT_PORT = 9229

    val httpHost: HttpHost

    override fun toString(): String {
        return if (port != DEFAULT_PORT){
            httphost!!.hostName + ":" + port
        } else {
            httphost!!.hostName
        }
    }
    companion object {
        private const val serialVersionUID = -6650701828361907957L
    }

    init {
        Args.notNull(httphost, "HTTP host")
        httpHost = httphost!!
    }
}
