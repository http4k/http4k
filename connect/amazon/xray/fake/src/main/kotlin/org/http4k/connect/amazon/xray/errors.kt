package org.http4k.connect.amazon.xray

import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.BAD_REQUEST

private fun error(status: Status, type: String, message: String) = Response(status)
    .header("x-amzn-ErrorType", type)
    .body(XRayMoshi.asFormatString(mapOf("Message" to message)))

internal fun invalidRequest(message: String) = error(BAD_REQUEST, "InvalidRequestException", message)
