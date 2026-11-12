package boxoffice

import boxoffice.domain.Actor
import boxoffice.domain.Layout
import boxoffice.domain.SeatRef
import boxoffice.domain.ShowSeat
import boxoffice.feature.BoxOfficeFeature
import boxoffice.store.Theatres
import org.http4k.fanbelt.testing.TestTenant
import org.http4k.fanbelt.testing.Verdict
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SeatsTest {
    private val tenant = TestTenant()
    private val boxOffice = BoxOfficeFeature(tenant.runtime.clock)

    init {
        Theatres(tenant.infra.storage).create(tenant.scope.tenant, tenant.runtime.clock, tenant.random, SHOW to Layout(rows = 2, seatsPerRow = 3))
    }

    @Test
    fun `a well-formed seat the show doesn't have can't be reserved`() {
        val show = tenant.read(boxOffice.shows).first().id
        val ann = Actor.Fan(viewerNamed("ann"))

        assertEquals(Verdict.Accepted, tenant.execute(boxOffice.reserve, ann, ShowSeat(show, SeatRef.of("B3"))))
        assertEquals(Verdict.Rejected("No such seat at this show"), tenant.execute(boxOffice.reserve, ann, ShowSeat(show, SeatRef.of("C1"))))
    }
}