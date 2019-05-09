package org.http4k.contract

import org.http4k.format.Json

typealias Render<NODE> = Json<NODE>.() -> NODE

interface SecurityRenderer {
    fun <NODE> full(security: Security): Render<NODE>?
    fun <NODE> ref(security: Security): Render<NODE>?
}