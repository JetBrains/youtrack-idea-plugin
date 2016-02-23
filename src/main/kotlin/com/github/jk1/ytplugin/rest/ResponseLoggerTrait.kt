package com.github.jk1.ytplugin.rest

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.google.gson.JsonSyntaxException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.JDOMUtil
import org.apache.commons.httpclient.HttpMethod
import org.jdom.Element
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*

interface ResponseLoggerTrait {

    val logger: Logger

    fun HttpMethod.responseBodyAsLoggedString(): String {
        val response = responseBodyAsString
        if (logger.isDebugEnabled){
            try {
                val header = getRequestHeader("Content-Type")
                val contentType = if (header == null) "text/plain" else header.elements[0].name.toLowerCase(Locale.ENGLISH)
                if (contentType.contains("xml")) {
                    logXml(response)
                } else if (contentType.contains("json")) {
                    logJson(response)
                } else {
                    logger.debug(response)
                }
            } catch (e: IOException) {
                logger.error(e)
            }
        }
        return response
    }

    fun HttpMethod.responseBodyAsLoggedStream(): InputStream {
        if (logger.isDebugEnabled){
            return ByteArrayInputStream(responseBodyAsLoggedString().toByteArray("UTF-8"))
        } else {
            return responseBodyAsStream
        }
    }

    fun logXml(element: Element) {
        if (logger.isDebugEnabled) {
            logger.debug("\n${JDOMUtil.createOutputter("\n").outputString(element)}")
        }
    }

    fun logXml(xml: InputStream) {
        if (logger.isDebugEnabled) {
            try {
                logger.debug("\n" + JDOMUtil.createOutputter("\n").outputString(JDOMUtil.loadDocument(xml)))
            } catch (e: Exception) {
                logger.debug(e)
            }
        }
    }

    fun logJson(json: JsonElement) {
        if (logger.isDebugEnabled) {
            try {
                val e = GsonBuilder().setPrettyPrinting().create()
                logger.debug("\n" + e.toJson(json))
            } catch (e: JsonSyntaxException) {
                logger.debug("Malformed JSON\n" + json)
            }
        }
    }


    private fun logXml(xml: String) {
        if (logger.isDebugEnabled) {
            try {
                logger.debug("\n" + JDOMUtil.createOutputter("\n").outputString(JDOMUtil.loadDocument(xml)))
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