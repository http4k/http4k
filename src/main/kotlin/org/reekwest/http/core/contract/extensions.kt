package org.reekwest.http.core.contract

import org.reekwest.http.core.HttpMessage
import java.nio.ByteBuffer

fun String.toByteBuffer(): ByteBuffer = ByteBuffer.wrap(this.toByteArray())

fun <A, B, C> Function1<A, B>.then(next: Function1<B, C>): Function1<A, C> = { next.invoke(this.invoke(it)) }

fun <T : HttpMessage> T.with(vararg modifiers: (T) -> T): T = modifiers.fold(this, { memo, next -> next(memo) })

