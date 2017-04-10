package org.reekwest.http.core.body

import org.reekwest.http.core.Body
import org.reekwest.http.core.HttpMessage

interface BodyRepresentation<T> {
    fun from(body: Body?): T
    fun to(value: T): Body
}

fun <T> HttpMessage.extract(representation: BodyRepresentation<T>): T = representation.from(body)
