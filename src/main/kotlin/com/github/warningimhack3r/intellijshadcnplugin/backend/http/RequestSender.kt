package com.github.warningimhack3r.intellijshadcnplugin.backend.http

import java.net.HttpURLConnection
import java.net.URL

// Credit to: https://gist.github.com/GrzegorzDyrda/be47602fc855a52fba240dd2c2adc2d5
object RequestSender {

    /**
     * Sends an HTTP request to the given [url], using the given HTTP [method]. The request can also
     * include custom [headers] and [body].
     *
     * Returns the [Response] object containing [statusCode][Response.statusCode],
     * [headers][Response.headers] and [body][Response.body].
     */
    fun sendRequest(url: String, method: String = "GET", headers: Map<String, String>? = null, body: String? = null): Response {
        val conn = URL(url).openConnection() as HttpURLConnection

        with(conn) {
            requestMethod = method
            doOutput = body != null
            headers?.forEach(::setRequestProperty)
        }

        if (body != null) {
            conn.outputStream.use {
                it.write(body.toByteArray())
            }
        }

        val responseBody = conn.inputStream.use { it.readBytes() }.toString(Charsets.UTF_8)

        return Response(conn.responseCode, conn.headerFields, responseBody)
    }

    data class Response(val statusCode: Int, val headers: Map<String, List<String>>? = null, val body: String? = null)
}
