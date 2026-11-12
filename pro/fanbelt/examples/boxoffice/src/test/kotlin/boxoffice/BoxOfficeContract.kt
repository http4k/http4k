package boxoffice

import boxoffice.domain.OrderStatus
import boxoffice.domain.SeatRef
import boxoffice.domain.ShowTitle
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Duration

val SHOW = ShowTitle.of("Midnight Owls")

private val F11 = SeatRef.of("F11")
private val F12 = SeatRef.of("F12")

abstract class BoxOfficeContract {
    abstract val driver: BoxOfficeDriver

    @Test
    fun `a seat reserved by one fan shows as taken for everyone watching`() {
        val ann = driver.fan("ann")
        val bob = driver.fan("bob")
        bob.watches(SHOW)

        assertEquals(Verdict.Accepted, ann.reserved(SHOW, F12))

        eventually { assertEquals(ShowSeen(mine = emptySet(), taken = setOf(F12)), bob.sees(SHOW)) }
        eventually { assertEquals(ShowSeen(mine = setOf(F12), taken = emptySet()), ann.sees(SHOW)) }
    }

    @Test
    fun `when two fans go for the same seat, the second is told someone just took it`() {
        val ann = driver.fan("ann")
        val bob = driver.fan("bob")

        assertEquals(Verdict.Accepted, ann.reserved(SHOW, F12))
        assertEquals(Verdict.Rejected("Someone just took F12"), bob.reserved(SHOW, F12))
    }

    @Test
    fun `a reservation lapses after five minutes, and the seat is free for someone else`() {
        val ann = driver.fan("ann")
        val bob = driver.fan("bob")
        bob.watches(SHOW)
        ann.reserved(SHOW, F12)
        eventually { assertEquals(setOf(F12), bob.sees(SHOW).taken) }

        driver.tick(Duration.ofMinutes(5))

        eventually { assertEquals(emptySet<SeatRef>(), bob.sees(SHOW).taken) }
        eventually { assertEquals(Verdict.Accepted, bob.reserved(SHOW, F12)) }
    }

    @Test
    fun `only the fan who reserved a seat can release it, and releasing it frees it for everyone`() {
        val ann = driver.fan("ann")
        val bob = driver.fan("bob")
        bob.watches(SHOW)
        ann.reserved(SHOW, F12)
        eventually { assertEquals(listOf(F12), ann.basket(SHOW).reserved) }

        assertEquals(Verdict.Denied, bob.releases(SHOW, F12))
        assertEquals(Verdict.Accepted, ann.releases(SHOW, F12))

        eventually { assertEquals(emptySet<SeatRef>(), bob.sees(SHOW).taken) }
    }

    @Test
    fun `fans see how many seats are left and who else is watching`() {
        val ann = driver.fan("ann")
        val bob = driver.fan("bob")
        ann.watches(SHOW)
        bob.watches(SHOW)

        bob.reserved(SHOW, F12)

        eventually { assertEquals(HeaderSeen(seatsLeft = 95, watching = setOf(ann.name, bob.name)), ann.seesHeader(SHOW)) }
    }

    @Test
    fun `checking out keeps the seats, and the tickets are confirmed once payment is taken`() {
        val ann = driver.fan("ann")
        ann.reserved(SHOW, F11)
        ann.reserved(SHOW, F12)

        assertEquals(Verdict.Accepted, ann.checksOut(SHOW))
        driver.tick(Duration.ofMinutes(6))

        eventually { assertEquals(BasketSeen(emptyList(), listOf(TicketsSeen(listOf(F11, F12), OrderStatus.Confirmed))), ann.basket(SHOW)) }
        eventually { assertEquals(setOf(F11, F12), ann.sees(SHOW).mine) }
    }

    @Test
    fun `a declined payment frees the seats for everyone`() {
        val ann = driver.fan("ann")
        val bob = driver.fan("bob")
        bob.watches(SHOW)
        ann.reserved(SHOW, F12)
        driver.cardsAreDeclined()

        ann.checksOut(SHOW)
        driver.tick(Duration.ofSeconds(2))

        eventually { assertEquals(listOf(TicketsSeen(listOf(F12), OrderStatus.Declined)), ann.basket(SHOW).tickets) }
        eventually { assertEquals(emptySet<SeatRef>(), bob.sees(SHOW).taken) }
    }

    @Test
    fun `there is nothing to check out without reserving a seat`() {
        assertEquals(Verdict.Rejected("Reserve some seats first"), driver.fan("ann").checksOut(SHOW))
    }
}