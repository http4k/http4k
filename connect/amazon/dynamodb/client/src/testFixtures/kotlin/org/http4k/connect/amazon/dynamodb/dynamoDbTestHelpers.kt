package org.http4k.connect.amazon.dynamodb

import java.time.Duration
import java.time.Instant

fun waitUntil(test: () -> Boolean, failureMessage: String, timeout: Duration) {
    val waitStart = Instant.now()
    while (Duration.between(waitStart, Instant.now()) < timeout) {
        if (test()) {
            return
        }
        Thread.sleep(1000)
    }
    throw IllegalStateException(failureMessage)
}
