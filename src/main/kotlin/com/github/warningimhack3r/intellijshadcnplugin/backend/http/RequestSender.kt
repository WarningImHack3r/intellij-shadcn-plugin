package com.github.warningimhack3r.intellijshadcnplugin.backend.http

import com.intellij.openapi.diagnostic.logger
import com.intellij.util.applyIf
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object RequestSender {
    private val log = logger<RequestSender>()

    /**
     * Sends an HTTP request to the given [url], using the given HTTP [method]. The request can also
     * include custom [headers] and [body].
     *
     * Returns the [Response] object containing [statusCode][Response.statusCode],
     * [headers][Response.headers] and [body][Response.body].
     */
    fun sendRequest(
        url: String, method: String = "GET", headers: Map<String, String>? = null, body: String? = null
    ): Response {
        log.debug("Sending $method request to $url")
        val request = HttpRequest.newBuilder(URI(url))
            .method(method, body?.let {
                HttpRequest.BodyPublishers.ofString(it)
            } ?: HttpRequest.BodyPublishers.noBody())
            .applyIf(headers != null) {
                headers?.forEach { (key, value) -> header(key, value) }
                this
            }
            .build()
        log.debug(
            "Request method: ${request.method()}, headers: ${
                request.headers().map()
            }, body:${
                if (body != null) {
                    "\n"
                } else ""
            }${body?.take(100)}${if ((body?.length ?: 0) > 100) "..." else ""}"
        )
        HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build()
            .send(request, HttpResponse.BodyHandlers.ofString()).let { response ->
                return Response(response.statusCode(), response.headers().map(), response.body()).also {
                    log.debug(
                        "Request to $url returned ${it.statusCode} (${it.body.length} bytes):${
                            if (it.body.isNotEmpty()) {
                                "\n"
                            } else ""
                        }${it.body.take(100)}${if (it.body.length > 100) "..." else ""}"
                    )
                }
            }
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
