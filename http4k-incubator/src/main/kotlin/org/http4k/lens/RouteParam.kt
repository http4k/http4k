package org.http4k.lens

sealed interface RouteParam {
    data class Body<OUT>(val meta: Meta, val example: OUT?)
    data class NonBody(val meta: Meta)
}
