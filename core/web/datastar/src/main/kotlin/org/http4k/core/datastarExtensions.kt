package org.http4k.core

import org.http4k.lens.DATASTAR_REQUEST
import org.http4k.lens.Header
import org.http4k.routing.asRouter

val Request.Companion.isDatastar get() = Request::isDatastar.asRouter()

fun Request.isDatastar() = Header.DATASTAR_REQUEST(this)
