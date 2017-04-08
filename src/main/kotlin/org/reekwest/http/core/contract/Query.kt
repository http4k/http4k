package org.reekwest.http.core.contract

import org.reekwest.http.core.Request
import org.reekwest.http.core.queries
import org.reekwest.http.core.query

object Query {
    fun optional(name: String) = object : MessagePart<Request, String?> {
        override fun toString(): String = "optional query $name"
        override fun get(msg: Request) = msg.query(name)
    }

    fun required(name: String) = object : MessagePart<Request, String> {
        override fun toString(): String = "required query $name"
        override fun get(msg: Request) = msg.query(name) ?: throw Missing(this)
    }

    object multi {
        fun optional(name: String) = object : MessagePart<Request, List<String?>> {
            override fun toString(): String = "optional multi query $name"
            override fun get(msg: Request) = msg.queries(name)
        }

        fun required(name: String) = object : MessagePart<Request, List<String?>> {
            override fun toString(): String = "required multi query $name"
            override fun get(msg: Request): List<String?> {
                val values = msg.queries(name)
                return if (values.isEmpty()) throw Missing(this) else values
            }
        }
    }
}

