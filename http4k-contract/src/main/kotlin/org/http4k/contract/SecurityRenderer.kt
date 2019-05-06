package org.http4k.contract

import org.http4k.format.Json

interface SecurityRenderer {
    fun <NODE> full(security: Security): Json<NODE>.() -> NODE
    fun <NODE> ref(security: Security): Json<NODE>.() -> NODE
}