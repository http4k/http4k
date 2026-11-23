package org.http4k.fanbelt.page

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.fanbelt.acmeWorkspace
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.connect
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.state.DerivedState
import org.http4k.fanbelt.state.State
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch

class RegionOnVirtualThreadsTest {
    private data class Shown(val value: String) : ViewModel

    private val infra = AppInfra(AppRuntime()).apply { start() }
    private val topic = Topic.of("t")
    private val workspace = acmeWorkspace(infra)
    private val released = CountDownLatch(1)

    private fun page(state: State<Unit, String>) =
        Page(workspace, Layout(listOf(Region("r", ::render, state) { model, _ -> Shown(model) }), { "" }) { _, _ -> Shown("") })

    private fun render(model: ViewModel) = """<p id="r">${(model as Shown).value}</p>"""

    private fun connect(page: Page, sending: () -> Unit = {}) {
        page.connect(Request(GET, "/")) { sending() }
    }

    private fun bothFinish(waiting: () -> Unit) {
        try {
            val waiter = Thread.ofVirtual().start(waiting)
            val releaser = Thread.ofVirtual().start { released.countDown() }
            assertThat("a virtual thread couldn't run while a region waited", releaser.join(Duration.ofSeconds(2)), equalTo(true))
            assertThat("the waiting region never finished", waiter.join(Duration.ofSeconds(2)), equalTo(true))
        } finally {
            released.countDown()
        }
    }

    @Test
    fun `a region waiting for its model leaves other virtual threads free to run`() {
        val slow = DerivedState<Unit, String>("shown") { ctx, _ ->
            ctx.watch(topic)
            released.await()
            "v1"
        }

        bothFinish { connect(page(slow)) }
    }

    @Test
    fun `a viewer slow to take a view leaves other virtual threads free to run`() {
        val shown = DerivedState<Unit, String>("shown") { ctx, _ ->
            ctx.watch(topic)
            "v1"
        }

        bothFinish { connect(page(shown)) { released.await() } }
    }
}
