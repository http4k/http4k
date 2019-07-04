package org.http4k.cloudnative

import org.http4k.core.Status
import org.http4k.core.Status.Companion.CLIENT_TIMEOUT
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.GATEWAY_TIMEOUT
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.UNAUTHORIZED

/**
 * This hierarchy of exceptions should be used to indicate that an upstream remote system has failed with a
 * non-successful status code which caused us to stop processing. They are designed to be used with the
 * Server and Client filters which will allow automatic handling and propagation of erroneous responses from
 * upstream.
 */
open class UpstreamRequestFailed(val status: Status, message: String) : RuntimeException(message + ". Caused by (${status.code})")

class NotFound(message: String) : UpstreamRequestFailed(NOT_FOUND, message)

class ClientTimeout(message: String) : UpstreamRequestFailed(CLIENT_TIMEOUT, message)

class Unauthorized(message: String) : UpstreamRequestFailed(UNAUTHORIZED, message)

class Forbidden(message: String) : UpstreamRequestFailed(FORBIDDEN, message)

class GatewayTimeout(message: String) : UpstreamRequestFailed(GATEWAY_TIMEOUT, message)
