package org.http4k.cloudnative

/**
 * This hierarchy of exceptions should be used to indicate that an upstream remote system has failed with a
 * non-successful status code which caused us to stop processing. They are designed to be used with the
 * Server and Client filters which will allow automatic handling and propagation of erroneous responses from
 * upstream.
 */
open class UpstreamRequestFailed(message: String) : RuntimeException(message)

class ServiceUnavailable(message: String) : UpstreamRequestFailed(message)

class NotFound(message: String) : UpstreamRequestFailed(message)

class InternalServerError(message: String) : UpstreamRequestFailed(message)

class Conflict(message: String) : UpstreamRequestFailed(message)

class ClientTimeout(message: String) : UpstreamRequestFailed(message)

class BadRequest(message: String) : UpstreamRequestFailed(message)

class BadGateway(message: String) : UpstreamRequestFailed(message)