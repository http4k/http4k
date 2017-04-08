package org.reekwest.http.core.contract

import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response

interface MessagePart<in M : HttpMessage, out T> {
    operator fun get(msg: M): T

    fun <X> map(next: (T) -> X): MessagePart<M, X> {
        val pre = this
        return object : MessagePart<M, X> {
            override fun get(msg: M): X = try { next(pre[msg]) } catch (e: Exception) { throw Invalid(pre) }
        }
    }
}

operator fun <T> Request.get(param: MessagePart<Request, T>) = param[this]

operator fun <T> Response.get(param: MessagePart<Response, T>) = param[this]
