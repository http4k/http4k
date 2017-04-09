package org.reekwest.http.core.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.queries
import org.reekwest.http.core.query

object Query {
    fun optional(name: String, description: String? = null) = Optional<Request, String>(Meta(name, "query", description)) { it.query(name) }
    fun required(name: String, description: String? = null) = Required<Request, String>(Meta(name, "query", description)) { it.query(name) }

    object multi {
        fun optional(name: String, description: String? = null) = Optional<Request, List<String?>>(Meta(name, "query", description)) { it.queries(name) }
        fun required(name: String, description: String? = null) = Required<Request, List<String?>>(Meta(name, "query", description)) {
            val values = it.queries(name)
            if (values.isEmpty()) null else values
        }
    }
}
