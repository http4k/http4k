package org.reekwest.http.core.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.header
import org.reekwest.http.core.headerValues

object Header {
    fun optional(name: String, description: String? = null) = Optional<Request, String>(Meta(name, "header", description)) { it.header(name) }
    fun required(name: String, description: String? = null) = Required<Request, String>(Meta(name, "header", description)) { it.header(name) }

    object multi {
        fun optional(name: String, description: String? = null) = Optional<Request, List<String?>>(Meta(name, "header", description)) { it.headerValues(name) }
        fun required(name: String, description: String? = null) = Required<Request, List<String?>>(Meta(name, "header", description)) {
            val values = it.headerValues(name)
            if (values.isEmpty()) null else values
        }
    }
}
