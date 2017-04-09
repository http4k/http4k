package org.reekwest.http.core.contract

import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response

abstract class MsgPart<in IN : HttpMessage, in OUT, out FINAL>(val meta: Meta, private val spec: Spec<IN, OUT>) {
    operator fun get(m: IN): FINAL = try {
        convert(spec.fn(m, meta.name))
    } catch (e: Missing) {
        throw e
    } catch (e: Exception) {
        throw Invalid(meta)
    }

    abstract internal fun convert(o: List<OUT?>?): FINAL
}

operator fun <T, FINAL> Request.get(param: MsgPart<Request, T, FINAL>): FINAL = param[this]
operator fun <T, FINAL> Response.get(param: MsgPart<Response, T, FINAL>): FINAL = param[this]
