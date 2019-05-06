package org.http4k.contract

import org.http4k.format.Json

interface SecurityRenderer {
    fun <NODE> full(json: Json<NODE>, security: Security): NODE
    fun <NODE> ref(json: Json<NODE>, security: Security): NODE
}