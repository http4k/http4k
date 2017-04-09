package org.reekwest.http.core.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.header
import org.reekwest.http.core.headerValues

object Header {
    fun optional(name: String) = Optional<Request, String>("header") { it.header(name) }
    fun required(name: String) = Required<Request, String>("header") { it.header(name) }

    object multi {
        fun optional(name: String) = Optional<Request, List<String?>>("header") { it.headerValues(name) }
        fun required(name: String) = Required<Request, List<String?>>("header") {
        val values = it.headerValues(name)
            if (values.isEmpty()) null else values
        }
    }
}
