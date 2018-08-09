package org.http4k.core

class Status internal constructor(val code: Int, val description: String, private val clientGenerated: Boolean = false) {

    constructor(code: Int, description: String) : this(code, description, false)

    companion object {
        private val INFORMATIONAL = 100..199
        val CONTINUE = Status(100, "Continue")
        val SWITCHING_PROTOCOLS = Status(101, "Switching Protocols")

        private val SUCCESSFUL = 200..299
        val OK = Status(200, "OK")
        val CREATED = Status(201, "Created")
        val ACCEPTED = Status(202, "Accepted")
        val NON_AUTHORITATIVE_INFORMATION = Status(203, "Non-Authoritative Information")
        val NO_CONTENT = Status(204, "No Content")
        val RESET_CONTENT = Status(205, "Reset Content")
        val PARTIAL_CONTENT = Status(206, "Partial Content")

        private val REDIRECTION = 300..399
        val MULTIPLE_CHOICES = Status(300, "Multiple Choices")
        val MOVED_PERMANENTLY = Status(301, "Moved Permanently")
        val FOUND = Status(302, "Found")
        val SEE_OTHER = Status(303, "See Other")
        val NOT_MODIFIED = Status(304, "Not Modified")
        val USE_PROXY = Status(305, "Use Proxy")
        val TEMPORARY_REDIRECT = Status(307, "Temporary Redirect")
        val PERMANENT_REDIRECT = Status(308, "Permanent Redirect")

        private val CLIENT_ERROR = 400..499
        val BAD_REQUEST = Status(400, "Bad Request")
        val UNSATISFIABLE_PARAMETERS = BAD_REQUEST.description("Unsatisfiable Parameters")
        val UNAUTHORIZED = Status(401, "Unauthorized")
        val PAYMENT_REQUIRED = Status(402, "Payment Required")
        val FORBIDDEN = Status(403, "Forbidden")
        val NOT_FOUND = Status(404, "Not Found")
        val METHOD_NOT_ALLOWED = Status(405, "Method Not Allowed")
        val NOT_ACCEPTABLE = Status(406, "Not Acceptable")
        val PROXY_AUTHENTICATION_REQUIRED = Status(407, "Proxy Authentication Required")
        val REQUEST_TIMEOUT = Status(408, "Request Timeout")
        val CONFLICT = Status(409, "Conflict")
        val GONE = Status(410, "Gone")
        val LENGTH_REQUIRED = Status(411, "Length Required")
        val PRECONDITION_FAILED = Status(412, "Precondition Failed")
        val REQUEST_ENTITY_TOO_LARGE = Status(413, "Request Entity Too Large")
        val REQUEST_URI_TOO_LONG = Status(414, "Request-URI Too Long")
        val UNSUPPORTED_MEDIA_TYPE = Status(415, "Unsupported Media Type")
        val REQUESTED_RANGE_NOT_SATISFIABLE = Status(416, "Requested Range Not Satisfiable")
        val EXPECTATION_FAILED = Status(417, "Expectation Failed")
        val I_M_A_TEAPOT = Status(418, "I'm a teapot") //RFC2324
        val UNPROCESSABLE_ENTITY = Status(422, "Unprocessable Entity")
        val TOO_MANY_REQUESTS = Status(429, "Too many requests")

        private val SERVER_ERROR = 500..599
        val INTERNAL_SERVER_ERROR = Status(500, "Internal Server Error")
        val NOT_IMPLEMENTED = Status(501, "Not Implemented")
        val BAD_GATEWAY = Status(502, "Bad Gateway")
        val SERVICE_UNAVAILABLE = Status(503, "Service Unavailable")
        val CONNECTION_REFUSED = Status(503, "Connection Refused", true)
        val UNKNOWN_HOST = Status(503, "Unknown Host", true)
        val GATEWAY_TIMEOUT = Status(504, "Gateway Timeout")
        val CLIENT_TIMEOUT = Status(504, "Client Timeout", true)
        val HTTP_VERSION_NOT_SUPPORTED = Status(505, "HTTP Version Not Supported")
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