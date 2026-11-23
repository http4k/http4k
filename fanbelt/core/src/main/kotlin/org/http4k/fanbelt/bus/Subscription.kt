package org.http4k.fanbelt.bus

/**
 * A subscription or presence lease; close it to end it.
 */
fun interface Subscription : AutoCloseable
