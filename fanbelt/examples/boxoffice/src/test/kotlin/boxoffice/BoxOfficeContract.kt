package boxoffice

import boxoffice.domain.OrderStatus
import boxoffice.domain.SeatRef
import boxoffice.domain.ShowTitle
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
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

        assertThat(ann.reserved(SHOW, F12), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.sees(SHOW), equalTo(ShowSeen(mine = emptySet(), taken = setOf(F12)))) }
        eventually { assertThat(ann.sees(SHOW), equalTo(ShowSeen(mine = setOf(F12), taken = emptySet()))) }
    }

    @Test
    fun `when two fans go for the same seat, the second is told someone just took it`() {
        val ann = driver.fan("ann")
        val bob = driver.fan("bob")

        assertThat(ann.reserved(SHOW, F12), equalTo(Verdict.Accepted))
        assertThat(bob.reserved(SHOW, F12), equalTo(Verdict.Rejected("Someone just took F12")))
    }

    @Test
    fun `a reservation lapses after five minutes, and the seat is free for someone else`() {
        val ann = driver.fan("ann")
        val bob = driver.fan("bob")
        bob.watches(SHOW)
        ann.reserved(SHOW, F12)
        eventually { assertThat(bob.sees(SHOW).taken, equalTo(setOf(F12))) }

        driver.tick(Duration.ofMinutes(5))

        eventually { assertThat(bob.sees(SHOW).taken, equalTo(emptySet<SeatRef>())) }
        eventually { assertThat(bob.reserved(SHOW, F12), equalTo(Verdict.Accepted)) }
    }

    @Test
    fun `only the fan who reserved a seat can release it, and releasing it frees it for everyone`() {
        val ann = driver.fan("ann")
        val bob = driver.fan("bob")
        bob.watches(SHOW)
        ann.reserved(SHOW, F12)
        eventually { assertThat(ann.basket(SHOW).reserved, equalTo(listOf(F12))) }

        assertThat(bob.releases(SHOW, F12), equalTo(Verdict.Denied))
        assertThat(ann.releases(SHOW, F12), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.sees(SHOW).taken, equalTo(emptySet<SeatRef>())) }
    }

    @Test
    fun `fans see how many seats are left and who else is watching`() {
        val ann = driver.fan("ann")
        val bob = driver.fan("bob")
        ann.watches(SHOW)
        bob.watches(SHOW)

        bob.reserved(SHOW, F12)

        eventually { assertThat(ann.seesHeader(SHOW), equalTo(HeaderSeen(seatsLeft = 95, watching = setOf(ann.name, bob.name)))) }
    }

    @Test
    fun `checking out keeps the seats, and the tickets are confirmed once payment is taken`() {
        val ann = driver.fan("ann")
        ann.reserved(SHOW, F11)
        ann.reserved(SHOW, F12)

        assertThat(ann.checksOut(SHOW), equalTo(Verdict.Accepted))
        driver.tick(Duration.ofMinutes(6))

        eventually { assertThat(ann.basket(SHOW), equalTo(BasketSeen(emptyList(), listOf(TicketsSeen(listOf(F11, F12), OrderStatus.Confirmed))))) }
        eventually { assertThat(ann.sees(SHOW).mine, equalTo(setOf(F11, F12))) }
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

        eventually { assertThat(ann.basket(SHOW).tickets, equalTo(listOf(TicketsSeen(listOf(F12), OrderStatus.Declined)))) }
        eventually { assertThat(bob.sees(SHOW).taken, equalTo(emptySet<SeatRef>())) }
    }

    @Test
    fun `there is nothing to check out without reserving a seat`() {
        assertThat(driver.fan("ann").checksOut(SHOW), equalTo(Verdict.Rejected("Reserve some seats first")))
    }
}