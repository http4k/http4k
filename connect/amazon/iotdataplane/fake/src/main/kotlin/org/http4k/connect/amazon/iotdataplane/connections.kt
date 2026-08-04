package org.http4k.connect.amazon.iotdataplane

import org.http4k.connect.amazon.iotdataplane.model.ClientId
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.routing.path

internal fun Request.clientId() = ClientId.of(path("clientId")!!)

/**
 * The fake holds no MQTT connections, so every client is an unknown one. The AWS SDK turns this
 * status and error header into a ResourceNotFoundException.
 */
internal fun connectionNotFound(clientId: ClientId) = Response(NOT_FOUND)
    .header("x-amzn-ErrorType", "ResourceNotFoundException")
    .body("""{"message":"No connection found for client: '${clientId.value}'"}""")
