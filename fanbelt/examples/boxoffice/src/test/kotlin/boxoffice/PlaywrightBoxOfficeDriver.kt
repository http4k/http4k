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
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import dev.forkhandles.time.DeterministicScheduler
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.playwright.newPage
import org.http4k.fanbelt.testing.playwright.pumping
import org.http4k.fanbelt.testing.playwright.textAs
import org.http4k.fanbelt.testing.playwright.verdictAt
import org.http4k.playwright.Http4kBrowser
import org.http4k.server.Jetty
import org.http4k.server.uri
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.AfterEach
import java.time.Duration
import java.util.Random

class PlaywrightBoxOfficeDriver(private val storage: DocumentStorage = InMemoryDocumentStorage()) : BoxOfficeDriver, AutoCloseable {
    private val scheduler = DeterministicScheduler()
    private val pump = scheduler.pumping()
    private val clock = scheduler.toClock()
    private val provider = FakePaymentProvider()
    private val server = App(AppRuntime(clock = clock, scheduler = scheduler, http = provider)) { AppInfra(it, storage) }.asServer(Jetty(0))
    private val playwright = Playwright.create()
    private val browser: Http4kBrowser

    init {
        Theatres(storage).create(TenantId.of("acme"), clock, Random(1), SHOW to Layout(rows = 8, seatsPerRow = 12))
        server.start()
        browser = Http4kBrowser(playwright.chromium().launch(), server.uri())
    }

    override fun fan(name: String): Fan = PlaywrightFan(viewerNamed(name))

    override fun tick(by: Duration) = scheduler.tick(by)

    override fun cardsAreDeclined() = provider.declines()

    override fun close() {
        pump.close()
        playwright.close()
        server.stop()
    }

    private inner class PlaywrightFan(private val id: ViewerId) : Fan {
        override val name = Viewer(id).name
        private val page: Page by lazy { browser.newPage(viewerCookie(id)) }
        private var on: String? = null
        private val paths = mutableMapOf<ShowTitle, String>()

        override fun watches(show: ShowTitle) = visit(showPath(show))

        override fun reserved(show: ShowTitle, seat: SeatRef): Verdict {
            visit(showPath(show))
            return page.verdictAt("p.error") { page.locator("#seat-${seat.value}").click() }
        }

        override fun releases(show: ShowTitle, seat: SeatRef): Verdict {
            visit(showPath(show))
            if (page.locator("#release-${seat.value}").count() == 0) return Verdict.Unavailable
            return page.verdictAt("p.error") { page.locator("#release-${seat.value}").click() }
        }

        override fun checksOut(show: ShowTitle): Verdict {
            visit(showPath(show))
            return page.verdictAt("p.error") { page.locator("button.checkout").click() }
        }

        override fun sees(show: ShowTitle): ShowSeen {
            visit(showPath(show))
            val reserved = page.locator("#seats .seat").all().mapNotNull { seat ->
                val reservedBy = seat.getAttribute("data-reservedBy").orEmpty()
                if (reservedBy.isEmpty()) null else SeatRef.of(seat.getAttribute("id")!!.removePrefix("seat-")) to ViewerId.parse(reservedBy)
            }
            return ShowSeen(
                reserved.filter { it.second == id }.map { it.first }.toSet(),
                reserved.filter { it.second != id }.map { it.first }.toSet(),
            )
        }

        override fun seesHeader(show: ShowTitle): HeaderSeen {
            visit(showPath(show))
            return HeaderSeen(page.locator("#header .left").innerText().toInt(), page.locator("#header li").allInnerTexts().toSet())
        }

        override fun basket(show: ShowTitle): BasketSeen {
            visit(showPath(show))
            return BasketSeen(
                page.locator("#basket .reserved .seat-ref").all().map { it.textAs(SeatRef) },
                page.locator("#basket .tickets li").all().map { TicketsSeen(it.locator(".seats").innerText().split(" ").map { ref -> SeatRef.of(ref) }, it.locator(".status").textAs<OrderStatus>()) },
            )
        }

        private fun showPath(title: ShowTitle) = paths.getOrPut(title) {
            visit("")
            page.locator("#shows li a").all().first { it.textAs(ShowTitle) == title }.getAttribute("href")!!
        }

        private fun visit(path: String) {
            if (on == path) return
            page.navigate("/theatres/acme/$path")
            page.locator("body[data-init]").waitFor()
            on = path
        }
    }
}

class BoxOfficePlaywrightTest : BoxOfficeContract, PortBasedTest {
    override val driver = PlaywrightBoxOfficeDriver()

    @AfterEach
    fun close() = driver.close()
}
