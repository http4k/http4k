package org.http4k.fanbelt.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.store.Identified
import org.http4k.fanbelt.store.Table
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.invoke
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.infrastructure.InMemoryCluster
import org.junit.jupiter.api.Test

class GivenStorageTest {
    private class PinId private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<PinId>(::PinId)
    }

    private data class Pin(override val id: PinId) : Identified<PinId>

    private object Board : Feature<Table<PinId, Pin>, Unit>({ tables -> tables(PINS) }) {
        val pin by operation { pins, _, id: String ->
            pins.insert(Pin(PinId.of(id)))
            Changes.none()
        }
    }

    private val storage = InMemoryDocumentStorage()

    private fun pins() = storage.read { it.tables(TenantId.of("acme"))<PinId, Pin>(PINS).page().items }

    @Test
    fun `a test tenant keeps its rows in the storage it's given`() {
        TestTenant(storage = storage).execute(Board.pin, "p1")

        assertThat(pins(), equalTo(listOf(Pin(PinId.of("p1")))))
    }

    @Test
    fun `a cluster's nodes keep their rows in the storage it's given`() {
        val cluster = InMemoryCluster(storage = storage)

        cluster.node()(cluster.runtime).apply { start() }.scope(TenantId.of("acme")).execute(Board.pin, Unit, "p1")

        assertThat(pins(), equalTo(listOf(Pin(PinId.of("p1")))))
    }
}

private val PINS = TableName.of("pins")
