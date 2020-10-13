package com.github.jk1.ytplugin.rest

import com.github.jk1.ytplugin.logger
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.util.JDOMUtil
import org.apache.commons.httpclient.HttpMethodBase
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

interface ResponseLoggerTrait {

    fun HttpMethodBase.responseBodyAsLoggedString(): String {
        val response = getResponseBodyAsString(25 * 1024 * 1024)
        if (logger.isDebugEnabled){
            try {
                val header = getRequestHeader("Content-Type")
                val contentType = if (header == null) "text/plain" else header.elements[0].name.toLowerCase(Locale.ENGLISH)
                when {
                    contentType.contains("xml") -> logXml(response)
                    contentType.contains("json") -> logJson(response)
                    else -> logger.debug(response)
                }
            } catch (e: IOException) {
                logger.error(e)
            }
        }
        return response
    }

    fun HttpMethodBase.responseBodyAsLoggedStream(): InputStream {
        return if (logger.isDebugEnabled){
            ByteArrayInputStream(responseBodyAsLoggedString().toByteArray(Charsets.UTF_8))
        } else {
            responseBodyAsStream
        }
    }

    private fun logXml(xml: String) {
        if (logger.isDebugEnabled) {
            try {
                logger.debug("\n" + JDOMUtil.createOutputter("\n").outputString(JDOMUtil.load(xml)))
            } catch (e: Exception) {
                logger.debug(e)
            }
        }
    }

    private fun logJson(json: String) {
        if (logger.isDebugEnabled) {
            try {
                val builder = GsonBuilder().setPrettyPrinting().create()
                logger.debug("\n${builder.toJson(builder.fromJson(json, JsonElement::class.java))}")
            } catch (e: JsonSyntaxException) {
                logger.debug("Malformed JSON\n$json")
            }
        }
    }
}