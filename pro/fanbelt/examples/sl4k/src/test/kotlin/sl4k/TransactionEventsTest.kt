package sl4k

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.events.MetadataEvent
import org.http4k.events.ProtocolEvent
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.Tenants
import org.http4k.fanbelt.testing.TestRuntime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TransactionEventsTest {
    private val runtime = TestRuntime()
    private val brokenStorage = object : DocumentStorage {
        override fun <R> write(work: (Tenants) -> R): R = error("storage is down")
    }
    private val http = App(runtime, { AppInfra(it, brokenStorage) }).handler.http!!

    @Test
    fun `a request that fails is reported with the status it was answered with`() {
        http(Request(GET, "/teams/demo/"))

        val transactions = runtime.events.map { if (it is MetadataEvent) it.event else it }.filterIsInstance<ProtocolEvent.Incoming>()
        assertEquals(listOf(INTERNAL_SERVER_ERROR), transactions.map { it.status })
    }
}
