package org.http4k.core

class Status internal constructor(val code: Int, val description: String, private val clientGenerated: Boolean = false) {

    constructor(code: Int, description: String?) : this(code, description ?: descriptions.getOrDefault(code, "Description not available"), false)

    companion object {
        private val INFORMATIONAL = 100..199
        @JvmField val CONTINUE = Status(100, "Continue")
        @JvmField val SWITCHING_PROTOCOLS = Status(101, "Switching Protocols")

        private val SUCCESSFUL = 200..299
        @JvmField val OK = Status(200, "OK")
        @JvmField val CREATED = Status(201, "Created")
        @JvmField val ACCEPTED = Status(202, "Accepted")
        @JvmField val NON_AUTHORITATIVE_INFORMATION = Status(203, "Non-Authoritative Information")
        @JvmField val NO_CONTENT = Status(204, "No Content")
        @JvmField val RESET_CONTENT = Status(205, "Reset Content")
        @JvmField val PARTIAL_CONTENT = Status(206, "Partial Content")

        private val REDIRECTION = 300..399
        @JvmField val MULTIPLE_CHOICES = Status(300, "Multiple Choices")
        @JvmField val MOVED_PERMANENTLY = Status(301, "Moved Permanently")
        @JvmField val FOUND = Status(302, "Found")
        @JvmField val SEE_OTHER = Status(303, "See Other")
        @JvmField val NOT_MODIFIED = Status(304, "Not Modified")
        @JvmField val USE_PROXY = Status(305, "Use Proxy")
        @JvmField val TEMPORARY_REDIRECT = Status(307, "Temporary Redirect")
        @JvmField val PERMANENT_REDIRECT = Status(308, "Permanent Redirect")

        private val CLIENT_ERROR = 400..499
        @JvmField val BAD_REQUEST = Status(400, "Bad Request")
        @JvmField val UNSATISFIABLE_PARAMETERS = BAD_REQUEST.description("Unsatisfiable Parameters")
        @JvmField val UNAUTHORIZED = Status(401, "Unauthorized")
        @JvmField val PAYMENT_REQUIRED = Status(402, "Payment Required")
        @JvmField val FORBIDDEN = Status(403, "Forbidden")
        @JvmField val NOT_FOUND = Status(404, "Not Found")
        @JvmField val METHOD_NOT_ALLOWED = Status(405, "Method Not Allowed")
        @JvmField val NOT_ACCEPTABLE = Status(406, "Not Acceptable")
        @JvmField val PROXY_AUTHENTICATION_REQUIRED = Status(407, "Proxy Authentication Required")
        @JvmField val REQUEST_TIMEOUT = Status(408, "Request Timeout")
        @JvmField val CONFLICT = Status(409, "Conflict")
        @JvmField val GONE = Status(410, "Gone")
        @JvmField val LENGTH_REQUIRED = Status(411, "Length Required")
        @JvmField val PRECONDITION_FAILED = Status(412, "Precondition Failed")
        @JvmField val REQUEST_ENTITY_TOO_LARGE = Status(413, "Request Entity Too Large")
        @JvmField val REQUEST_URI_TOO_LONG = Status(414, "Request-URI Too Long")
        @JvmField val UNSUPPORTED_MEDIA_TYPE = Status(415, "Unsupported Media Type")
        @JvmField val REQUESTED_RANGE_NOT_SATISFIABLE = Status(416, "Requested Range Not Satisfiable")
        @JvmField val EXPECTATION_FAILED = Status(417, "Expectation Failed")
        @JvmField val I_M_A_TEAPOT = Status(418, "I'm a teapot") //RFC2324
        @JvmField val UNPROCESSABLE_ENTITY = Status(422, "Unprocessable Entity")
        @JvmField val UPGRADE_REQUIRED = Status(426, "Upgrade Required")
        @JvmField val TOO_MANY_REQUESTS = Status(429, "Too many requests")

        private val SERVER_ERROR = 500..599
        @JvmField val INTERNAL_SERVER_ERROR = Status(500, "Internal Server Error")
        @JvmField val NOT_IMPLEMENTED = Status(501, "Not Implemented")
        @JvmField val BAD_GATEWAY = Status(502, "Bad Gateway")
        @JvmField val SERVICE_UNAVAILABLE = Status(503, "Service Unavailable")
        @JvmField val CONNECTION_REFUSED = Status(503, "Connection Refused", true)
        @JvmField val UNKNOWN_HOST = Status(503, "Unknown Host", true)
        @JvmField val GATEWAY_TIMEOUT = Status(504, "Gateway Timeout")
        @JvmField val CLIENT_TIMEOUT = Status(504, "Client Timeout", true)
        @JvmField val HTTP_VERSION_NOT_SUPPORTED = Status(505, "HTTP Version Not Supported")

        val descriptions: Map<Int, String> = listOf(
            CONTINUE, SWITCHING_PROTOCOLS,
            OK, CREATED, ACCEPTED, NON_AUTHORITATIVE_INFORMATION, NO_CONTENT, RESET_CONTENT, PARTIAL_CONTENT,
            MULTIPLE_CHOICES, MOVED_PERMANENTLY, FOUND, SEE_OTHER, NOT_MODIFIED, USE_PROXY, TEMPORARY_REDIRECT, PERMANENT_REDIRECT,
            BAD_REQUEST, UNSATISFIABLE_PARAMETERS, UNAUTHORIZED, PAYMENT_REQUIRED, FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED,
            NOT_ACCEPTABLE, PROXY_AUTHENTICATION_REQUIRED, REQUEST_TIMEOUT, CONFLICT, GONE, LENGTH_REQUIRED, PRECONDITION_FAILED,
            REQUEST_ENTITY_TOO_LARGE, REQUEST_URI_TOO_LONG, UNSUPPORTED_MEDIA_TYPE, REQUESTED_RANGE_NOT_SATISFIABLE,
            EXPECTATION_FAILED, I_M_A_TEAPOT, UNPROCESSABLE_ENTITY, UPGRADE_REQUIRED, TOO_MANY_REQUESTS,
            INTERNAL_SERVER_ERROR, NOT_IMPLEMENTED, BAD_GATEWAY, SERVICE_UNAVAILABLE, CONNECTION_REFUSED, UNKNOWN_HOST,
            GATEWAY_TIMEOUT, CLIENT_TIMEOUT, HTTP_VERSION_NOT_SUPPORTED).associateBy({it.code},  {it.description})
    }

    val successful by lazy { SUCCESSFUL.contains(code) }
    val informational by lazy { INFORMATIONAL.contains(code) }
    val redirection by lazy { REDIRECTION.contains(code) }
    val clientError by lazy { CLIENT_ERROR.contains(code) || clientGenerated }
    val serverError by lazy { SERVER_ERROR.contains(code) }

    fun description(newDescription: String) = Status(code, newDescription, clientGenerated)

    override fun hashCode(): Int = code.hashCode()
    override fun toString(): String = "$code $description"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Status

        if (code != other.code) return false
        if (clientGenerated != other.clientGenerated) return false

        return true
    }
}
