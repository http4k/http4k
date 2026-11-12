package boxoffice

import boxoffice.domain.Layout
import boxoffice.domain.OrderStatus
import boxoffice.domain.SeatRef
import boxoffice.domain.ShowTitle
import boxoffice.domain.Viewer
import boxoffice.domain.ViewerId
import boxoffice.fakes.FakePaymentProvider
import boxoffice.store.Theatres
import boxoffice.web.viewerCookie
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.TestRuntime
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.browser
import org.http4k.fanbelt.testing.provesServerRefuses
import org.http4k.fanbelt.testing.start
import org.http4k.fanbelt.testing.textAs
import org.http4k.fanbelt.testing.verdict
import org.openqa.selenium.By
import java.time.Duration
import java.util.Random

class UiBoxOfficeDriver(
    private val scheduler: DeterministicScheduler = DeterministicScheduler(start),
    private val provider: FakePaymentProvider = FakePaymentProvider(),
    storage: DocumentStorage = InMemoryDocumentStorage(),
    private val nodes: List<Node> = listOf(App(TestRuntime(scheduler, http = provider)) { AppInfra(it, storage) }),
) : BoxOfficeDriver {
    init {
        Theatres(storage).create(TenantId.of("acme"), TestRuntime(scheduler).clock, Random(1), SHOW to Layout(rows = 8, seatsPerRow = 12))
        nodes.forEach(Node::start)
    }

    private var arrivals = 0

    override fun fan(name: String): Fan = fan(name, "acme")

    fun fan(name: String, theatre: String): Fan = UiFan(viewerNamed(name), theatre)

    override fun tick(by: Duration) {
        scheduler.tick(by)
        scheduler.runUntilIdle()
    }

    override fun cardsAreDeclined() = provider.declines()

    private inner class UiFan(private val id: ViewerId, private val theatre: String) : Fan {
        override val name = Viewer(id).name
        private val browser = browser(nodes[arrivals++ % nodes.size], scheduler, viewerCookie(id))
        private var on: String? = null
        private val paths = mutableMapOf<ShowTitle, String>()

        override fun watches(show: ShowTitle) = visitShow(show)

        override fun reserved(show: ShowTitle, seat: SeatRef): Verdict {
            visitShow(show)
            browser.findElement(By.id("seat-${seat.value}")).click()
            return browser.verdict(By.cssSelector("p.error"))
        }

        override fun releases(show: ShowTitle, seat: SeatRef): Verdict {
            visitShow(show)
            val button = browser.findElements(By.id("release-${seat.value}")).firstOrNull()
                ?: return nodes.first().provesServerRefuses("/theatres/$theatre/${showPath(show)}/seats/${seat.value}/release", viewerCookie(id))
            button.click()
            return browser.verdict(By.cssSelector("p.error"))
        }

        override fun checksOut(show: ShowTitle): Verdict {
            visitShow(show)
            browser.findElement(By.cssSelector("button.checkout")).click()
            return browser.verdict(By.cssSelector("p.error"))
        }

        override fun sees(show: ShowTitle): ShowSeen {
            visitShow(show)
            val reserved = browser.findElements(By.cssSelector("#seats .seat")).mapNotNull { seat ->
                val reservedBy = seat.getAttribute("data-reservedBy").orEmpty()
                if (reservedBy.isEmpty()) null else SeatRef.of(seat.getAttribute("id")!!.removePrefix("seat-")) to ViewerId.parse(reservedBy)
            }
            return ShowSeen(
                reserved.filter { it.second == id }.map { it.first }.toSet(),
                reserved.filter { it.second != id }.map { it.first }.toSet(),
            )
        }

        override fun seesHeader(show: ShowTitle): HeaderSeen {
            visitShow(show)
            return HeaderSeen(
                browser.findElement(By.cssSelector("#header .left")).text.toInt(),
                browser.findElements(By.cssSelector("#header li")).map { it.text }.toSet(),
            )
        }

        override fun basket(show: ShowTitle): BasketSeen {
            visitShow(show)
            return BasketSeen(
                browser.findElements(By.cssSelector("#basket .reserved .seat-ref")).map { it.textAs(SeatRef) },
                browser.findElements(By.cssSelector("#basket .tickets li")).map {
                    TicketsSeen(it.findElement(By.className("seats")).text.split(" ").map { ref -> SeatRef.of(ref) }, it.findElement(By.className("status")).textAs<OrderStatus>())
                },
            )
        }

        private fun visitShow(title: ShowTitle) = visit(showPath(title))

        private fun showPath(title: ShowTitle) = paths.getOrPut(title) {
            visit("")
            browser.findElements(By.cssSelector("#shows li a")).first { it.textAs(ShowTitle) == title }.getAttribute("href")!!
        }

        private fun visit(path: String) {
            if (on == path) return
            browser.get("http://localhost/theatres/$theatre/$path")
            on = path
        }
    }
}