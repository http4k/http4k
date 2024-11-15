package org.http4k.connect.kafka.rest.extensions

import dev.forkhandles.result4k.Result4k

/**
 * Helper to enable processing of Records.
 */
fun interface RecordConsumer<T : Any> : (T) -> Result4k<Unit, Exception>
