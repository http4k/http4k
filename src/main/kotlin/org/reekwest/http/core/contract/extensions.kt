package org.reekwest.http.core.contract

import java.nio.ByteBuffer

fun String.toByteBuffer(): ByteBuffer = ByteBuffer.wrap(this.toByteArray())

fun <A, B, C> Function1<A, B>.then(next: Function1<B, C>): Function1<A, C> = { next.invoke(this.invoke(it)) }


