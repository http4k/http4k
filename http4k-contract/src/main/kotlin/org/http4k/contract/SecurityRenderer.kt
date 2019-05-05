package org.http4k.contract

interface SecurityRenderer<NODE> {
    fun full(security: Security): NODE
    fun ref(security: Security): NODE

    companion object
}