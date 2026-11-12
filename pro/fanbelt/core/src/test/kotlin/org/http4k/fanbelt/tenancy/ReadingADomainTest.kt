/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.tenancy

import dev.forkhandles.time.DeterministicScheduler
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.Tenants
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class ReadingADomainTest {
    private object Greetings : Feature<String, String>({ "hello" })

    private class Transactions(private val storage: DocumentStorage) : DocumentStorage {
        val taken = mutableListOf<String>()

        override fun <R> write(work: (Tenants) -> R): R {
            taken += "write"
            return storage.write(work)
        }

        override fun <R> read(work: (Tenants) -> R): R {
            taken += "read"
            return storage.read(work)
        }
    }

    @Test
    fun `reading a domain takes a read transaction`() {
        val storage = Transactions(InMemoryDocumentStorage())
        val scope = AppInfra(AppRuntime(scheduler = DeterministicScheduler(Instant.EPOCH)), storage).apply { start() }.scope(TenantId.of("acme"))

        val greeting = scope.read(Greetings) { it.uppercase() }

        assertEquals("HELLO", greeting)
        assertEquals(listOf("read"), storage.taken)
    }
}
