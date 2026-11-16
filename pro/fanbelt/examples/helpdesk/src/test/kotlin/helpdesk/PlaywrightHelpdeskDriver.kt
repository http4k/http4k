package helpdesk

import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.WaitForSelectorState.DETACHED
import helpdesk.domain.Actor
import helpdesk.domain.CustomerEmail
import helpdesk.domain.NewTicket
import helpdesk.domain.Org
import helpdesk.domain.ReplyBody
import helpdesk.domain.Status
import helpdesk.domain.Subject
import helpdesk.domain.TicketId
import helpdesk.domain.Viewer
import helpdesk.domain.ViewerId
import helpdesk.fakes.FakeEmailProvider
import helpdesk.fakes.Fakes
import helpdesk.feature.TicketsFeature
import helpdesk.store.Orgs
import helpdesk.web.viewerCookie
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.MovableClock
import org.http4k.fanbelt.testing.MovableScheduler
import org.http4k.fanbelt.testing.RealBrowser
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.accepted
import org.http4k.fanbelt.testing.asVerdict
import org.http4k.fanbelt.testing.posting
import org.http4k.fanbelt.testing.provesServerRefuses
import org.http4k.fanbelt.testing.start
import org.http4k.fanbelt.testing.textAs
import org.http4k.fanbelt.testing.verdict
import org.http4k.server.Jetty
import org.http4k.server.uri
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.time.Duration

class PlaywrightHelpdeskDriver(private val storage: DocumentStorage = InMemoryDocumentStorage()) : HelpdeskDriver, AutoCloseable {
    private val clock = MovableClock()
    private val provider = FakeEmailProvider()
    private val runtime = AppRuntime(clock = clock, scheduler = MovableScheduler(clock), http = Fakes(provider))
    private val policy = orgPolicy(runtime.http, clock)
    private lateinit var infra: AppInfra
    private val app = App(runtime, { AppInfra(it, storage).also { infra = it } }, provider, policy)
    private val server = app.asServer(Jetty(0)).start()
    private val browser = RealBrowser(server.uri()).apply { start() }
    private val tickets = TicketsFeature(runtime, policy)
    private val customers = infra.scope(TenantId.of("acme"))

    init {
        Orgs(storage).create(TenantId.of("acme"), Org(start))
    }

    override fun agent(name: String): Agent = PlaywrightAgent(viewerNamed(name).also { policy.appoint(TenantId.of("acme"), it) })

    override fun orgRemoves(agent: String) = policy.dismiss(TenantId.of("acme"), viewerNamed(agent))

    override fun policyRechecked() = clock.tick(policy.recheckEvery)

    override fun customerOpens(subject: Subject, from: CustomerEmail): TicketId {
        return customers.execute(tickets.open, Actor.Customer(from), NewTicket(subject, ReplyBody.of("Please help"))).accepted()
    }

    override fun customerReplies(ticket: TicketId, from: CustomerEmail, text: ReplyBody) =
        customers.execute(tickets.customerReply, Actor.Customer(from), ticket to text.value).asVerdict()

    override fun emailsTo(customer: CustomerEmail) = provider.sent.filter { it.to == customer.value }.map { it.subject }

    override fun jobsRun() = clock.tick(Duration.ofSeconds(1))

    override fun close() {
        browser.close()
        server.stop()
    }

    private inner class PlaywrightAgent(private val id: ViewerId) : Agent {
        override val name = Viewer(id).name
        private var page: Page? = null
        private var on: TicketId? = null

        override fun watches(ticket: TicketId) {
            visit(ticket).locator("#viewing li:has-text(\"(you)\")").waitFor()
        }

        override fun leaves(ticket: TicketId) {
            if (on != ticket) return
            page?.context()?.close()
            page = null
            on = null
        }

        override fun takes(ticket: TicketId) {
            val take = button(ticket, "Take it")
            if (take.count() == 0) return
            val posted = visit(ticket).posting { take.click() }
            if (posted.ok()) take.waitFor(Locator.WaitForOptions().setState(DETACHED))
        }

        override fun replies(ticket: TicketId, text: String): Verdict {
            val page = visit(ticket)
            if (page.locator("form.reply").count() == 0) return page.provesServerRefuses("/orgs/acme/t/$ticket/reply", """{"reply":"$text"}""")
            return page.verdict("form.reply .error") {
                page.locator("form.reply textarea").fill(text)
                page.locator("form.reply button").click()
            }
        }

        override fun resolves(ticket: TicketId): Verdict {
            val resolve = button(ticket, "Resolve")
            if (resolve.count() == 0) return visit(ticket).provesServerRefuses("/orgs/acme/t/$ticket/status/Resolved")
            return visit(ticket).verdict("form.reply .error") { resolve.click() }
        }

        override fun sees(ticket: TicketId): TicketSeen {
            val page = visit(ticket)
            return TicketSeen(
                status = page.locator("#header .status").textAs<Status>(),
                assignee = page.locator("#header .assignee").allInnerTexts().firstOrNull(),
                replies = page.locator("#conversation .body").all().map { it.textAs(ReplyBody) },
                notification = page.locator("#header .notification").allInnerTexts().firstOrNull(),
                actions = page.locator("#header .actions button").allInnerTexts().mapNotNull { buttons[it.trim()] }.toSet(),
                viewers = page.locator("#viewing li").allInnerTexts().map { it.removeSuffix(" (you)") }.toSet(),
            )
        }

        override fun previewsCustomerOf(ticket: TicketId): List<Subject> {
            val page = visit(ticket)
            page.locator("#customer-$ticket").click()
            page.locator("#customer-card .subject").first().waitFor()
            return page.locator("#customer-card .subject").all().map { it.textAs(Subject) }
        }

        override fun finds(ticket: TicketId) = visit(ticket).locator("#header.gone").count() == 0

        override fun isOn(ticket: TicketId) = page?.apply { content() }?.url()?.endsWith("/t/$ticket") == true

        override fun opens(ticket: TicketId) =
            JavaHttpClient()(Request(GET, server.uri().path("/orgs/acme/t/$ticket")).cookie(viewerCookie(id))).status == OK

        override fun seesAgents(): Set<String> {
            val open = page ?: browser.open("/orgs/acme/agents/$id", viewerCookie(id)).also { page = it }
            open.navigate("/orgs/acme/agents/$id")
            on = null
            return open.locator("#agents li a").allInnerTexts().map { it.substringBefore(" ·").trim() }.toSet()
        }

        private fun visit(ticket: TicketId): Page {
            val open = page ?: browser.open("/orgs/acme/t/$ticket", viewerCookie(id)).also { page = it; on = ticket }
            if (on != ticket) open.navigate("/orgs/acme/t/$ticket")
            on = ticket
            return open
        }

        private fun button(ticket: TicketId, label: String) =
            visit(ticket).locator("#header .actions button:text-is(\"$label\")")
    }
}

@EnabledIfEnvironmentVariable(named = "FANBELT_TEST_BROWSER", matches = ".+")
class HelpdeskPlaywrightTest : HelpdeskContract() {
    override val driver = PlaywrightHelpdeskDriver()

    @AfterEach
    fun close() = driver.close()
}
