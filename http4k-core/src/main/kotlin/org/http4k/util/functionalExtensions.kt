package org.http4k.util

/**
 * Compose any 2 simple functions together
 */
fun <A, B, C> ((A) -> B).then(fn: (B) -> C): (A) -> C = { fn(this(it)) }
