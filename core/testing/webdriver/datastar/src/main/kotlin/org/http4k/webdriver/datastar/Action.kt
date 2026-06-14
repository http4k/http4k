package org.http4k.webdriver.datastar

import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.lens.contentType

/**
 * A backend action (@get/@post/@put/@patch/@delete). Per the datastar v1 protocol, non-local
 * signals accompany every request: GET sends them in the 'datastar' query param, other methods
 * as a JSON body.
 */
internal data class Action(val method: Method, val path: String) {
    fun toRequest(signalsJson: String? = null): Request {
        val request = Request(method, path).header("datastar-request", "true")
        return when {
            signalsJson == null -> request
            method == GET -> request.query("datastar", signalsJson)
            else -> request.contentType(ContentType.APPLICATION_JSON).body(signalsJson)
        }
    }
}
