/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.lock

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

abstract class LocksContract {
    abstract val locks: Locks
    abstract val elsewhere: Locks

    private val name = "charge/acme/${System.nanoTime()}"

    @Test
    fun `a held lock is skipped by anyone else trying it`() {
        locks.tryLock(name)!!.use {
            assertThat(elsewhere.tryLock(name), equalTo(null))
        }
    }

    @Test
    fun `a released lock can be taken again`() {
        locks.tryLock(name)!!.close()

        assertThat(elsewhere.tryLock(name)?.also { it.close() } != null, equalTo(true))
    }

    @Test
    fun `locks with different names don't block each other`() {
        locks.tryLock(name)!!.use {
            assertThat(elsewhere.tryLock("$name-other")?.also { it.close() } != null, equalTo(true))
        }
    }
}
