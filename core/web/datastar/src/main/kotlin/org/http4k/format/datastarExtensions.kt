package org.http4k.format

import org.http4k.datastar.Signal

fun <T : Any> AutoMarshallingJson<*>.asDatastarSignal(t: T): Signal = Signal.of(asFormatString(t))
