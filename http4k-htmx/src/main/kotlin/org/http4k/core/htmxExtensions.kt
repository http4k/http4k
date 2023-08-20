package org.http4k.core

import org.http4k.lens.HX_REQUEST
import org.http4k.lens.Header
import org.http4k.lens.isHtmx
import org.http4k.routing.asRouter

val Request.Companion.isHtmx get() = Request::isHtmx.asRouter()

fun Request.isHtmx() = Header.HX_REQUEST(this)

val Status.Companion.STOP_POLLING get() = Status(286, "Stop Polling")
