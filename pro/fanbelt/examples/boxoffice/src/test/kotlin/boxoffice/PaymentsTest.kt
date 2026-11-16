package boxoffice

import boxoffice.domain.Actor
import boxoffice.domain.Layout
import boxoffice.domain.OrderId
import boxoffice.domain.OrderStatus.Confirmed
import boxoffice.domain.OrderStatus.Pending
import boxoffice.domain.SeatRef
import boxoffice.domain.ShowSeat
import boxoffice.feature.BoxOfficeFeature
import boxoffice.feature.OrdersFeature
import boxoffice.store.Theatres
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.core.body.form
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.TestTenant
import org.junit.jupiter.api.Test
import java.time.Duration

class PaymentsTest {
    private val ann = Actor.Fan(viewerNamed("ann"))

    private inner class Harness {
        val charged = mutableListOf<String>()
        var answer: Status = OK
        val runtime = TestRuntime(http = { request ->
            charged += request.form("order").orEmpty()
            Response(answer)
        })
        val boxOffice = BoxOfficeFeature(runtime.clock)
        val orders = OrdersFeature(boxOffice, runtime)
        val tenant = TestTenant(runtime, jobs = listOf(orders))

        init {
            Theatres(tenant.infra.storage).create(tenant.scope.tenant, runtime.clock, runtime.random, SHOW to Layout(rows = 8, seatsPerRow = 12))
        }

        fun checkedOut(): OrderId {
            val show = tenant.read(boxOffice.shows).first().id
            tenant.execute(boxOffice.reserve, ann, ShowSeat(show, SeatRef.of("F12")))
            tenant.scope.execute(orders.checkout, ann, show)
            tenant.settle()
            return tenant.scope.read(orders) { it.of(ann.id) }.single().id
        }

        fun statuses() = tenant.scope.read(orders) { it.of(ann.id) }.map { it.status }
    }

    @Test
    fun `checking out charges the card straight away`() {
        val app = Harness()

        app.checkedOut()

        assertThat(app.statuses(), equalTo(listOf(Confirmed)))
    }

    @Test
    fun `a charge that fails is tried again a second later`() {
        val app = Harness()
        app.answer = SERVICE_UNAVAILABLE
        app.checkedOut()
        app.answer = OK

        app.tenant.tick(Duration.ofMillis(999))
        val backingOff = app.statuses()
        app.tenant.tick(Duration.ofMillis(1))

        assertThat(backingOff to app.statuses(), equalTo(listOf(Pending) to listOf(Confirmed)))
    }

}
