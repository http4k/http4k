package org.http4k.fanbelt.tenancy

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import java.time.Instant
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.Tenants
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.junit.jupiter.api.Test

class ReadingAStateTest {
    private object Greetings : Feature<String, String>({ "hello" }) {
        val shouted by state { greeting -> greeting.uppercase() }
    }

    private class Transactions(private val storage: DocumentStorage) : DocumentStorage {
        override val locks get() = storage.locks

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
    fun `reading a state takes a read transaction`() {
        val storage = Transactions(InMemoryDocumentStorage())
        val scope = AppInfra(AppRuntime(scheduler = DeterministicScheduler(Instant.EPOCH)), storage).apply { start() }.scope(TenantId.of("acme"))

        val greeting = scope.read(Greetings.shouted)

        assertThat(greeting, equalTo("HELLO"))
        assertThat(storage.taken, equalTo(listOf("read")))
    }
}
