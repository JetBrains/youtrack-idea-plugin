package com.github.jk1.ytplugin.navigator

import fi.iki.elonen.NanoHTTPD
import java.io.ByteArrayInputStream

/**
 * These are actually gif files of different size used as an operation success markers in YouTrack.
 * Strangely enough, HTTP status codes mean very little in this 'contract'.
 */
private val successMarker = byteArrayOf(71, 73, 70, 56, 57, 97, 2, 0, 2, 0, -128, -1, 0, -1, -1, -1, 0, 0, 0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 68, 1, 0, 59);
private val failureMarker = byteArrayOf(71, 73, 70, 56, 57, 97, 1, 0, 1, 0, -128, -1, 0, -1, -1, -1, 0, 0, 0, 44, 0, 0, 0, 0, 1, 0, 1, 0, 0, 2, 2, 68, 1, 0, 59);

fun successResponse() : NanoHTTPD.Response {
    val response = NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK, "image/gif",
            ByteArrayInputStream(successMarker), successMarker.size.toLong())
    response.closeConnection(true)
    return response
}

fun errorResponse() : NanoHTTPD.Response {
    val response = NanoHTTPD.newFixedLengthResponse(
            NanoHTTPD.Response.Status.OK, "image/gif",
            ByteArrayInputStream(failureMarker), failureMarker.size.toLong())
    response.closeConnection(true)
    return response
}

fun customResponse(status: NanoHTTPD.Response.Status) : NanoHTTPD.Response {
    val response = NanoHTTPD.newFixedLengthResponse(status, "text/plain", null, 0)
    response.closeConnection(true)
    return response
}
