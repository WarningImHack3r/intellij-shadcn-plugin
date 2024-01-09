package com.github.warningimhack3r.intellijshadcnplugin.backend.http

import com.intellij.openapi.diagnostic.logger
import java.net.HttpURLConnection
import java.net.URL

// Credit to: https://gist.github.com/GrzegorzDyrda/be47602fc855a52fba240dd2c2adc2d5
object RequestSender {
    private val log = logger<RequestSender>()

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
            body?.let {
                outputStream.use {
                    it.write(body.toByteArray())
                }
            }
        }

        if (conn.responseCode in 300..399) {
            log.debug("Redirecting from ${conn.url} to ${conn.getHeaderField("Location")}")
            return sendRequest(conn.getHeaderField("Location"), method, mapOf(
                "Cookie" to conn.getHeaderField("Set-Cookie")
            ).filter { it.value != null }, body)
        }

        return Response(conn.responseCode, conn.headerFields, conn.inputStream.bufferedReader().readText())
    }

    data class Response(val statusCode: Int, val headers: Map<String, List<String>>, val body: String) {

        fun <T> ok(action: (Response) -> T): T? {
            if (statusCode in 200..299) {
                return action(this)
            }
            return null
        }
    }
}
