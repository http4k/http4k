package org.http4k.cloudnative

import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.GATEWAY_TIMEOUT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri

/**
 * This hierarchy of exceptions should be used to indicate that an upstream remote system has failed with a
 * non-successful status code which caused us to stop processing. They are designed to be used with the
 * Server and Client filters which will allow automatic handling and propagation of erroneous responses from
 * upstream.
 */
open class RemoteRequestFailed(val status: Status, message: String, uri: Uri? = null) : RuntimeException(
    "${uri?.toString()?.takeIf { it.isNotBlank() }?.let { "$it " }.orEmpty()}(${status.code})" +
        message.takeIf { it.isNotBlank() }
            ?.let { ":\n\t${it.replace("\t", "\t\t")}" }
            .orEmpty()
)

class NotFound(message: String, uri: Uri? = null) : RemoteRequestFailed(NOT_FOUND, message, uri)

class ClientTimeout(message: String, uri: Uri? = null) : RemoteRequestFailed(CLIENT_TIMEOUT, message, uri)

class Unauthorized(message: String, uri: Uri? = null) : RemoteRequestFailed(UNAUTHORIZED, message, uri)

class Forbidden(message: String, uri: Uri? = null) : RemoteRequestFailed(FORBIDDEN, message, uri)

class GatewayTimeout(message: String, uri: Uri? = null) : RemoteRequestFailed(GATEWAY_TIMEOUT, message, uri)
