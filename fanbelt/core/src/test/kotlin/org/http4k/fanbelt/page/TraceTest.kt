package org.http4k.fanbelt.page

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.state.Snapshot
import org.http4k.fanbelt.tenancy.TenantId
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class TraceTest {
    private val infra = AppInfra(AppRuntime()).apply { start() }
    private val scope = infra.scope(TenantId.of("t"))

    @AfterEach
    fun close() = infra.close()

    @Test
    fun `a closed trace hears nothing, however its reads raced`() {
        val reads = AtomicInteger()
        val heardAfterClose = AtomicInteger()
        val topics = (1..2000).flatMap { _ ->
            val start = CyclicBarrier(2)
            val read = mutableListOf<Topic>()
            var closed = false
            val trace = Trace(scope, { _ -> Topic.of("t-${reads.incrementAndGet()}").let { synchronized(read) { read += it }; Snapshot(Unit, setOf(it), emptySet(), null) } }) { if (closed) heardAfterClose.incrementAndGet() }
            listOf(thread { start.await(); trace.read() }, thread { start.await(); trace.read() }).forEach(Thread::join)
            closed = true
            trace.close()
            read
        }

        scope.bus.publish(topics.toSet())
        Thread.sleep(500)

        assertThat(heardAfterClose.get(), equalTo(0))
    }
}
