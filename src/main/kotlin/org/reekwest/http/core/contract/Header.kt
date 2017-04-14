package org.reekwest.http.core.contract

import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.header
import org.reekwest.http.core.headerValues

object Header : StringLensSpec<HttpMessage>("header",
    { request, name -> request.headerValues(name) },
    { req, name, values -> values.fold(req, { m, next -> m.header(name, next) }) }) {

    object Common {
        val CONTENT_TYPE = map(::ContentType).optional("Content-Type")
    }
}

