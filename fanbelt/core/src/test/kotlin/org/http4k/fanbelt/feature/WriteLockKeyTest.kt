package org.http4k.fanbelt.feature

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.LockKey
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.Tenants
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.junit.jupiter.api.Test
import java.time.Instant

class WriteLockKeyTest {
    private object Channels : Feature<Unit, String>({}) {
        val member by rule { _, _, _: String -> Allowed(Unit) }
        val signedIn by rule { _, _ -> Allowed(Unit) }

        val post by operation(member, { channel: String -> channel }) { _, _, _, _ -> Changes.none() }
        val create by operation(signedIn) { _, _, _: String, _ -> Changes.none() }
        val sweep by operation { _, _, _: String -> Changes.none() }
    }

    private class Locks(private val storage: DocumentStorage) : DocumentStorage by storage {
        val written = mutableListOf<LockKey?>()

        override fun <R> write(work: (Tenants) -> R): R = storage.write { tenants ->
            work(object : Tenants by tenants {
                override fun tables(tenant: TenantId, key: LockKey?): Tables {
                    written += key
                    return tenants.tables(tenant, key)
                }
            })
        }
    }

    @Test
    fun `an operation locks its rule's key, or the whole tenant when it has none`() {
        val storage = Locks(InMemoryDocumentStorage())
        val scope = AppInfra(AppRuntime(scheduler = DeterministicScheduler(Instant.EPOCH)), storage).apply { start() }.scope(TenantId.of("acme"))

        Channels.post.execute(scope, "ann", "random")
        Channels.create.execute(scope, "ann", "eng")
        Channels.sweep.execute(scope, "ann", "eng")

        assertThat(storage.written, equalTo(listOf(LockKey.of("random"), null, null)))
    }
}
