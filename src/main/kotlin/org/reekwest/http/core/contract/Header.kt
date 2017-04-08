package org.reekwest.http.core.contract

import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Request
import org.reekwest.http.core.header
import org.reekwest.http.core.headerValues

object Header {
    fun optional(name: String) = object : MessagePart<HttpMessage, String?> {
        override fun toString(): String = "optional header $name"
        override fun get(msg: HttpMessage) = msg.header(name)
    }

    fun required(name: String) = object : MessagePart<HttpMessage, String> {
        override fun toString(): String = "required header $name"
        override fun get(msg: HttpMessage) = msg.header(name) ?: throw Missing(this)
    }

    object multi {
        fun optional(name: String) = object : MessagePart<Request, List<String?>> {
            override fun toString(): String = "optional multi header $name"
            override fun get(msg: Request) = msg.headerValues(name)
        }

        fun required(name: String) = object : MessagePart<Request, List<String?>> {
            override fun toString(): String = "required multi query $name"
            override fun get(msg: Request): List<String?> {
                val values = msg.headerValues(name)
                return if (values.isEmpty()) throw Missing(this) else values
            }
        }
    }
}
