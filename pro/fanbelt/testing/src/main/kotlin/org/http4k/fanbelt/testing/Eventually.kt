/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import java.time.Duration

val untilAClosedTabIsNoticed: Duration = Duration.ofSeconds(6)

fun <T> eventually(within: Duration = Duration.ofSeconds(5), check: () -> T): T {
    val deadline = System.nanoTime() + within.toNanos()
    while (true) {
        try {
            return check()
        } catch (failure: AssertionError) {
            if (System.nanoTime() > deadline) throw failure
            Thread.sleep(25)
        } catch (failure: NoSuchElementException) {
            if (System.nanoTime() > deadline) throw failure
            Thread.sleep(25)
        }
    }
}
