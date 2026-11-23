package org.http4k.fanbelt.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.fanbelt.job.Jobs
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.server.PolyHandler
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class InMemoryClusterTest {
    class OrderId private constructor(value: String) : StringValue(value) {
        companion object : StringValueFactory<OrderId>(::OrderId)
    }

    private class Orders : Feature<Unit, Unit>({}) {
        val runs = AtomicInteger()
        val charge by job(OrderId) { _, _ ->
            runs.incrementAndGet()
            error("card service down")
        }
        val checkout by operation { _, _, id: String -> Changes.of(charge.topic(OrderId.of(id))) }
    }

    @Test
    fun `a node that dies runs none of its jobs again, so only a node still alive can finish them`() {
        val cluster = InMemoryCluster()
        val infra = cluster.node()(cluster.runtime)
        val orders = Orders()
        val node = Node(PolyHandler({ Response(OK) }), infra, Jobs(infra, orders.charge)).apply { start() }
        infra.scope(TenantId.of("acme")).execute(orders.checkout, Unit, "42")
        cluster.tick(Duration.ZERO)

        node.dies()
        cluster.tick(Duration.ofSeconds(5))

        assertThat(orders.runs.get(), equalTo(1))
    }
}
