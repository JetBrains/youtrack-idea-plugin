package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.logger
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.ide.util.PropertiesComponent
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets


fun obtainYouTrackConfiguration(url: String) {
    val builder = URIBuilder(url.trimEnd('/') + "/api/config")
    builder.addParameter("fields", "version,uuid")
    val method = HttpGet(builder.build())
    val client = SetupRepositoryConnector.setupHttpClient()

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
            instanceUUID = (json.get("uuid") ?: null).toString()
        }
    } catch (e: Exception) {
        logger.warn("invalid token or login, failed on configuration get: ${e.message}")
        logger.debug(e)
    }

    PropertiesComponent.getInstance().setValue("youtrack.version", instanceVersion.toString())
    PropertiesComponent.getInstance().setValue("youtrack.uuid", instanceUUID)

}
