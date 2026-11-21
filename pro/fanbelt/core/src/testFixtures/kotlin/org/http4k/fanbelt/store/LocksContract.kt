/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

abstract class LocksContract {
    abstract val locks: Locks
    abstract val elsewhere: Locks

    private val name = "charge/acme/${System.nanoTime()}"

    @Test
    fun `a held lock is skipped by anyone else trying it`() {
        val skipped = mutableListOf<Boolean>()

        val held = locks(name) { skipped += !elsewhere(name) {} }

        assertThat(held to skipped, equalTo(true to listOf(true)))
    }

    @Test
    fun `a released lock can be taken again`() {
        locks(name) {}

        assertThat(elsewhere(name) {}, equalTo(true))
    }

    @Test
    fun `a lock is released when its work throws`() {
        runCatching { locks(name) { error("failed") } }

        assertThat(elsewhere(name) {}, equalTo(true))
    }

    @Test
    fun `locks with different names don't block each other`() {
        val taken = mutableListOf<Boolean>()

        locks(name) { taken += elsewhere("$name-other") {} }

        assertThat(taken, equalTo(listOf(true)))
    }

    @Test
    fun `a held lock is skipped by another thread of the same node`() {
        val skipped = mutableListOf<Boolean>()

        locks(name) { thread { skipped += !locks(name) {} }.join() }

        assertThat(skipped, equalTo(listOf(true)))
    }
}
