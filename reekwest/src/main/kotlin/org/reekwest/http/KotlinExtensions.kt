package org.reekwest.http

import java.nio.ByteBuffer

fun ByteBuffer.asString(): String = String(array())
fun String.asByteBuffer(): ByteBuffer = ByteBuffer.wrap(this.toByteArray())
