package boxoffice

import boxoffice.domain.Actor
import boxoffice.domain.Layout
import boxoffice.domain.SeatRef
import boxoffice.domain.Shopper
import boxoffice.domain.ShowSeat
import boxoffice.domain.ShowTitle
import boxoffice.domain.Viewer
import boxoffice.domain.ViewerId
import boxoffice.fakes.FakePaymentProvider
import boxoffice.feature.BoxOfficeFeature
import boxoffice.feature.OrdersFeature
import boxoffice.jobs.PaymentJobs
import boxoffice.store.Theatres
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.TestTenant
import java.time.Duration

class DomainBoxOfficeDriver : BoxOfficeDriver {
    private val provider = FakePaymentProvider()
    private val tenant = TestTenant(TestRuntime(http = provider))
    private val boxOffice = BoxOfficeFeature(tenant.runtime.clock)
    private val orders = OrdersFeature(boxOffice, tenant.runtime.clock, tenant.random)

    init {
        Theatres(tenant.infra.storage).create(tenant.scope.tenant, tenant.runtime.clock, tenant.random, SHOW to Layout(rows = 8, seatsPerRow = 12))
        PaymentJobs(tenant.runtime, tenant.infra, orders).start()
    }

    override fun fan(name: String): Fan = DomainFan(viewerNamed(name))

    override fun tick(by: Duration) = tenant.tick(by)

    override fun cardsAreDeclined() = provider.declines()

    private inner class DomainFan(private val id: ViewerId) : Fan {
        override val name = Viewer(id).name
        private val actor = Actor.Fan(id)

        override fun watches(show: ShowTitle) {
            tenant.join(boxOffice.watching, showNamed(show), id)
            sees(show)
            seesHeader(show)
            basket(show)
        }

        override fun reserved(show: ShowTitle, seat: SeatRef) = tenant.execute(boxOffice.reserve, actor, ShowSeat(showNamed(show), seat))

        override fun releases(show: ShowTitle, seat: SeatRef) = tenant.execute(boxOffice.release, actor, ShowSeat(showNamed(show), seat))

        override fun checksOut(show: ShowTitle) = tenant.execute(orders.checkout, actor, showNamed(show))

        override fun sees(show: ShowTitle): ShowSeen {
            val seats = tenant.read(boxOffice.seatMap, showNamed(show))!!.seats
            return ShowSeen(
                seats.filter { it.reservedBy == id }.map { it.seat }.toSet(),
                seats.filter { it.reservedBy != null && it.reservedBy != id }.map { it.seat }.toSet(),
            )
        }

        override fun seesHeader(show: ShowTitle): HeaderSeen {
            val header = tenant.read(boxOffice.header, showNamed(show))!!
            return HeaderSeen(header.seatsLeft, header.watching.map { Viewer(it).name }.toSet())
        }

        override fun basket(show: ShowTitle): BasketSeen {
            val basket = tenant.read(orders.basket, Shopper(showNamed(show), id))
            return BasketSeen(basket.reserved, basket.tickets.map { TicketsSeen(it.seats, it.status) })
        }

        private fun showNamed(title: ShowTitle) = tenant.read(boxOffice.shows).first { it.title == title }.id
    }
}