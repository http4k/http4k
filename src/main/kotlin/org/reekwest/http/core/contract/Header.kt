package org.reekwest.http.core.contract

import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.header
import org.reekwest.http.core.headerValues

object Header : LensSpec<HttpMessage, String>("header",
    { request, name -> request.headerValues(name).mapNotNull { it -> it?.toByteBuffer() } },
    { req, name, values -> values.fold(req, { m, next -> m.header(name, String(next.array())) }) },
    { it -> String(it.array()) }, { it.toByteBuffer() }){

    object Common {
        val CONTENT_TYPE = map(::ContentType).optional("Content-Type")
    }
}

