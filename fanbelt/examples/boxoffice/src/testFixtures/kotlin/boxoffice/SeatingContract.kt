package boxoffice

import boxoffice.domain.SeatRef
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.http4k.fanbelt.testing.refusedAs
import org.junit.jupiter.api.Test
import java.time.Duration

private val F12 = SeatRef.of("F12")

interface SeatingContract {
    val driver: BoxOfficeDriver

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

        assertThat(bob.releases(SHOW, F12), refusedAs(Verdict.Denied))
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
}
