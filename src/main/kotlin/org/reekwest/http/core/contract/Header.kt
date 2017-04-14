package org.reekwest.http.core.contract

import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.headerValues

object Header : LensSpec<HttpMessage, String>("header",
    HttpMessage::headerValues,
    { msg, name, values -> TODO() }, { it }, { it }) {

    object Common {
        val CONTENT_TYPE = map(::ContentType).optional("Content-Type")
    }
}

