package org.reekwest.http.core.contract

import org.reekwest.http.core.ContentType
import org.reekwest.http.core.Request
import org.reekwest.http.core.headerValues

object Header : Spec<Request, String>("header", Request::headerValues) {
    object Common {
        val CONTENT_TYPE = map(::ContentType).optional("Content-Type")
    }
}