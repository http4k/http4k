package org.http4k.storyboard.datastar

import org.http4k.core.Method
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Request

internal data class Action(val method: Method, val path: String) {
    fun toRequest(): Request = Request(method, path).header("datastar-request", "true")
}

private val expressionPattern = Regex("""^@(get|post|put|delete)\('([^']*)'\)$""")

internal fun parseAction(expression: String): Action? {
    val match = expressionPattern.find(expression.trim()) ?: return null
    val (verb, path) = match.destructured
    val method = when (verb.lowercase()) {
        "get" -> GET
        "post" -> POST
        "put" -> PUT
        "delete" -> DELETE
        else -> return null
    }
    return Action(method, path)
}
