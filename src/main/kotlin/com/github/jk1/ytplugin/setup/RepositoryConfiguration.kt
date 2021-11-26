package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.logger
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets


data class RepositoryConfiguration(
    val version: Double?,
    val uuid: String?,
    val isHosted: Boolean
)

fun getYouTrackConfiguration(url: String): RepositoryConfiguration {
    val builder = URIBuilder(url.trimEnd('/') + "/api/config")
    builder.addParameter("fields", "version,uuid,hosted(hosted)")
    val method = HttpGet(builder.build())
    val client = SetupRepositoryConnector.setupHttpClient()


    var instanceIsHosted: Boolean = false
    var instanceUUID: String? = null
    var instanceVersion: Double? = null

    try {
        client.execute(method) {

            val reader = InputStreamReader(it.entity.content, StandardCharsets.UTF_8)
            val json: JsonObject = JsonParser.parseReader(reader).asJsonObject

            instanceVersion = if (json.get("version") == null || json.get("version").isJsonNull) {
                null
            } else {
                val version = json.get("version").asString.toDouble()
                logger.info("YouTrack version: $version")
                version
            }
            instanceIsHosted = json.get("hosted").asJsonObject.get("hosted").asBoolean
            instanceUUID = (json.get("uuid") ?: null).toString()
        }
    } catch (e: Exception) {
        logger.warn("invalid token or login, failed on configuration get: ${e.message}")
        logger.debug(e)
    }

    return RepositoryConfiguration(instanceVersion, instanceUUID, instanceIsHosted)
}
