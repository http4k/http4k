package org.http4k.core

import org.http4k.lens.DATASTAR_REQUEST
import org.http4k.lens.Header

val Request.isDatastar get() = Header.DATASTAR_REQUEST(this)
