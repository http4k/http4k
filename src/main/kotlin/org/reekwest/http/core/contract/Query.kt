package org.reekwest.http.core.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.queries
import org.reekwest.http.core.query

object Query {
    fun optional(name: String) = Optional<Request, String> { it.query(name) }
    fun required(name: String) = Required<Request, String> { it.query(name) }

    object multi {
        fun optional(name: String) = Optional<Request, List<String?>> { it.queries(name) }
        fun required(name: String) = Required<Request, List<String?>> { it.queries(name) }
    }
}
