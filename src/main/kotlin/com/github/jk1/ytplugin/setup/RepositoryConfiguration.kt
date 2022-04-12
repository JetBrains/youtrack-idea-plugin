package com.github.jk1.ytplugin.setup

import com.github.jk1.ytplugin.logger
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.ide.util.PropertiesComponent
import com.intellij.tasks.youtrack.YouTrackRepository
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets


fun obtainYouTrackConfiguration(repository: YouTrackRepository) {
    val builder = URIBuilder(repository.url.trimEnd('/') + "/api/config")
    builder.addParameter("fields", "version,uuid")
    val method = HttpGet(builder.build())
    val client = SetupRepositoryConnector.setupHttpClient(repository)

    try {
        client.execute(method) {

            val reader = InputStreamReader(it.entity.content, StandardCharsets.UTF_8)
            val json: JsonObject = JsonParser.parseReader(reader).asJsonObject

            processVersion(json)
            processUUID(json)
        }
    } catch (e: Exception) {
        logger.warn("invalid token or login, failed on configuration get: ${e.message}")
        logger.debug(e)
    }

}

fun processVersion(json: JsonObject): Double? {
    val version = if (json.get("version") == null || json.get("version").isJsonNull) {
        null
    } else {
        json.get("version").asString.toDouble()
    }
    logger.info("YouTrack version: $version")
    PropertiesComponent.getInstance().setValue("youtrack.version", version.toString())
    return version

}

fun processUUID(json: JsonObject): String {
    val uuid = (json.get("uuid") ?: null).toString()
    PropertiesComponent.getInstance().setValue("youtrack.uuid", uuid)
    return uuid
}

fun getInstanceVersion(): Double? {
    val stringVersion = PropertiesComponent.getInstance().getValue("youtrack.version")
    return if (stringVersion != null && stringVersion != "null") stringVersion.toDouble() else null
}

fun getInstanceUUID(): String? {
    val stringUUID =  PropertiesComponent.getInstance().getValue("youtrack.uuid")?.removeSurrounding("\"")
    return if (stringUUID != null && stringUUID != "null") stringUUID else null
}