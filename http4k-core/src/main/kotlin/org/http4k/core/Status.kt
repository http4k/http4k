package org.http4k.core

data class Status(val code: Int, val description: String) {
    companion object {
        private val INFORMATIONAL = 100..199
        val CONTINUE = status(100, "Continue")
        val SWITCHING_PROTOCOLS = status(101, "Switching Protocols")

        private val SUCCESSFUL = 200..299
        val OK = status(200, "OK")
        val CREATED = status(201, "Created")
        val ACCEPTED = status(202, "Accepted")
        val NON_AUTHORITATIVE_INFORMATION = status(203, "Non-Authoritative Information")
        val NO_CONTENT = status(204, "No Content")
        val RESET_CONTENT = status(205, "Reset Content")
        val PARTIAL_CONTENT = status(206, "Partial Content")

        private val REDIRECTION = 300..399
        val MULTIPLE_CHOICES = status(300, "Multiple Choices")
        val MOVED_PERMANENTLY = status(301, "Moved Permanently")
        val FOUND = status(302, "Found")
        val SEE_OTHER = status(303, "See Other")
        val NOT_MODIFIED = status(304, "Not Modified")
        val USE_PROXY = status(305, "Use Proxy")
        val TEMPORARY_REDIRECT = status(307, "Temporary Redirect")

        private val CLIENT_ERROR = 400..499
        val BAD_REQUEST = status(400, "Bad Request")
        val UNSATISFIABLE_PARAMETERS = BAD_REQUEST.copy(description = "Unsatisfiable Parameters")
        val UNAUTHORIZED = status(401, "Unauthorized")
        val PAYMENT_REQUIRED = status(402, "Payment Required")
        val FORBIDDEN = status(403, "Forbidden")
        val NOT_FOUND = status(404, "Not Found")
        val METHOD_NOT_ALLOWED = status(405, "Method Not Allowed")
        val NOT_ACCEPTABLE = status(406, "Not Acceptable")
        val PROXY_AUTHENTICATION_REQUIRED = status(407, "Proxy Authentication Required")
        val REQUEST_TIMEOUT = status(408, "Request Timeout")
        val CONFLICT = status(409, "Conflict")
        val GONE = status(410, "Gone")
        val LENGTH_REQUIRED = status(411, "Length Required")
        val PRECONDITION_FAILED = status(412, "Precondition Failed")
        val REQUEST_ENTITY_TOO_LARGE = status(413, "Request Entity Too Large")
        val REQUEST_URI_TOO_LONG = status(414, "Request-URI Too Long")
        val UNSUPPORTED_MEDIA_TYPE = status(415, "Unsupported Media Type")
        val REQUESTED_RANGE_NOT_SATISFIABLE = status(416, "Requested Range Not Satisfiable")
        val EXPECTATION_FAILED = status(417, "Expectation Failed")
        val I_M_A_TEAPOT = status(418, "I'm a teapot") //RFC2324

        private val SERVER_ERROR = 500..599
        val INTERNAL_SERVER_ERROR = status(500, "Internal Server Error")
        val NOT_IMPLEMENTED = status(501, "Not Implemented")
        val BAD_GATEWAY = status(502, "Bad Gateway")
        val SERVICE_UNAVAILABLE = status(503, "Service Unavailable")
        val CONNECTION_REFUSED = SERVICE_UNAVAILABLE.copy(description = "Connection Refused")
        val GATEWAY_TIMEOUT = status(504, "Gateway Timeout")
        val CLIENT_TIMEOUT = GATEWAY_TIMEOUT.copy(description = "Client Timeout")
        val HTTP_VERSION_NOT_SUPPORTED = status(505, "HTTP Version Not Supported")

        fun status(code: Int, description: String) = Status(code, description)
    }

    val successful by lazy { SUCCESSFUL.contains(code) }
    val informational by lazy { INFORMATIONAL.contains(code) }
    val redirection by lazy { REDIRECTION.contains(code) }
    val clientError by lazy { CLIENT_ERROR.contains(code) }
    val serverError by lazy { SERVER_ERROR.contains(code) }

    override fun equals(other: Any?): Boolean = other != null && other is Status && other.code == code
    override fun hashCode(): Int = code.hashCode()
    override fun toString(): String = "$code $description"
}