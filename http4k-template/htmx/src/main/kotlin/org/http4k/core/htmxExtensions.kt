package org.http4k.core

import org.http4k.lens.HX_REQUEST
import org.http4k.lens.Header

fun Request.isHtmx() = Header.HX_REQUEST(this)
