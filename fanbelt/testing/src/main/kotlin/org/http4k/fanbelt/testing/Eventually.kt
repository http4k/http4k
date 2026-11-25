package org.http4k.fanbelt.testing

import java.time.Duration

val untilAClosedTabIsNoticed: Duration = Duration.ofSeconds(6)

/**
 * Retries a check until it passes or times out.
 */
fun <T> eventually(within: Duration = Duration.ofSeconds(5), interval: Duration = Duration.ofMillis(25), check: () -> T): T {
    val deadline = System.nanoTime() + within.toNanos()
    while (true) {
        try {
            return check()
        } catch (failure: AssertionError) {
            if (System.nanoTime() > deadline) throw failure
            Thread.sleep(interval)
        } catch (failure: Exception) {
            if (System.nanoTime() > deadline) throw failure
            Thread.sleep(interval)
        }
    }
}
