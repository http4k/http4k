package boxoffice

import boxoffice.domain.OrderStatus
import boxoffice.domain.SeatRef
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.junit.jupiter.api.Test
import java.time.Duration

private val F11 = SeatRef.of("F11")
private val F12 = SeatRef.of("F12")

interface CheckoutContract {
    val driver: BoxOfficeDriver

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
