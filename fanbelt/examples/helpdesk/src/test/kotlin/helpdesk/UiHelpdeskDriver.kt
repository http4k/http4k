package helpdesk

import helpdesk.domain.Action
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
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.cookie
import org.http4k.core.then
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.accepted
import org.http4k.fanbelt.testing.asVerdict
import org.http4k.fanbelt.testing.infrastructure.TestRuntime
import org.http4k.fanbelt.testing.provesServerRefuses
import org.http4k.fanbelt.testing.webdriver.textAs
import org.http4k.fanbelt.testing.webdriver.toFanBeltBrowser
import org.http4k.fanbelt.testing.webdriver.verdict
import org.http4k.filter.ClientFilters
import org.http4k.testing.toHttpHandler
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import java.time.Duration
import java.time.Instant.ofEpochMilli

class UiHelpdeskDriver : HelpdeskDriver {
    private val provider = FakeEmailProvider()
    private val runtime = TestRuntime(http = Fakes(provider))
    private val storage = InMemoryDocumentStorage()
    private val infra = AppInfra(runtime, storage)
    private val policy = orgPolicy(runtime.http, runtime.clock)
    private val scheduler = runtime.scheduler
    private val app = App(runtime, { infra }, provider, policy).apply { start() }
    private val tickets = TicketsFeature(runtime, policy)
    private val customers = infra.scope(TenantId.of("acme"))

    init {
        Orgs(storage).create(TenantId.of("acme"), Org(ofEpochMilli(0)))
        scheduler.runUntilIdle()
    }

    override fun agent(name: String): UiAgent = UiAgent(viewerNamed(name).also { policy.appoint(TenantId.of("acme"), it) })

    override fun orgRemoves(agent: String) = policy.dismiss(TenantId.of("acme"), viewerNamed(agent))

    override fun policyRechecked() {
        scheduler.tick(policy.recheckEvery)
        scheduler.runUntilIdle()
    }

    fun providerIsUp() = provider.isUp

    override fun customerOpens(subject: Subject, from: CustomerEmail): TicketId {
        val opened = customers.execute(tickets.open, Actor.Customer(from), NewTicket(subject, ReplyBody.of("Please help")))
        scheduler.runUntilIdle()
        return opened.accepted()
    }

    override fun customerReplies(ticket: TicketId, from: CustomerEmail, text: ReplyBody): Verdict {
        val outcome = customers.execute(tickets.customerReply, Actor.Customer(from), ticket to text.value)
        scheduler.runUntilIdle()
        return outcome.asVerdict()
    }

    override fun emailsTo(customer: CustomerEmail) = provider.sent.filter { it.to == customer.value }.map { it.subject }

    override fun jobsRun() {
        scheduler.tick(Duration.ofSeconds(1))
        scheduler.runUntilIdle()
    }

    inner class UiAgent(private val id: ViewerId) : Agent {
        override val name = Viewer(id).name
        private val browser = ClientFilters.SetCookie(viewerCookie(id)).then(app.handler.toHttpHandler()).toFanBeltBrowser(scheduler)
        private var on: TicketId? = null

        override fun watches(ticket: TicketId) = visit(ticket)

        override fun leaves(ticket: TicketId) {
            if (on != ticket) return
            browser.close()
            scheduler.runUntilIdle()
            on = null
        }

        override fun takes(ticket: TicketId) {
            button(ticket, "Take it")?.click()
        }

        override fun replies(ticket: TicketId, text: String): Verdict {
            visit(ticket)
            val form = browser.findElements(By.cssSelector("form.reply")).firstOrNull()
                ?: return app.provesServerRefuses("/orgs/acme/t/$ticket/reply", viewerCookie(id), """{"reply":"$text"}""")
            form.findElement(By.tagName("textarea")).run {
                clear()
                sendKeys(text)
            }
            form.submit()
            return browser.verdict(By.cssSelector("form.reply .error"))
        }

        override fun resolves(ticket: TicketId): Verdict {
            val resolve = button(ticket, "Resolve") ?: return app.provesServerRefuses("/orgs/acme/t/$ticket/status/Resolved", viewerCookie(id))
            resolve.click()
            return browser.verdict(By.cssSelector("form.reply .error"))
        }

        override fun sees(ticket: TicketId): TicketSeen {
            visit(ticket)
            return TicketSeen(
                status = browser.findElement(By.cssSelector("#header .status")).textAs<Status>(),
                assignee = browser.findElements(By.cssSelector("#header .assignee")).firstOrNull()?.text,
                replies = browser.findElements(By.cssSelector("#conversation .body")).map { it.textAs(ReplyBody) },
                notification = browser.findElements(By.cssSelector("#header .notification")).firstOrNull()?.text,
                actions = browser.findElements(By.cssSelector("#header .actions button")).mapNotNull { buttons[it.text] }.toSet(),
                viewers = browser.findElements(By.cssSelector("#viewing li")).map { it.text.removeSuffix(" (you)") }.toSet(),
            )
        }

        override fun previewsCustomerOf(ticket: TicketId): List<Subject> {
            visit(ticket)
            browser.findElement(By.id("customer-$ticket")).click()
            return browser.findElements(By.cssSelector("#customer-card .subject")).map { it.textAs(Subject) }
        }

        override fun seesAgents(): Set<String> {
            browser.get("http://localhost/orgs/acme/agents/$id")
            on = null
            return browser.findElements(By.cssSelector("#agents li a")).map { it.text.substringBefore(" ·").trim() }.toSet()
        }

        override fun isOn(ticket: TicketId): Boolean {
            browser.settle()
            return browser.currentUrl.orEmpty().endsWith("/t/$ticket")
        }

        override fun opens(ticket: TicketId) =
            app.handler.http!!(Request(GET, "/orgs/acme/t/$ticket").cookie(viewerCookie(id))).status == OK

        fun gets(path: String) = app.handler.http!!(Request(GET, "/orgs/acme$path").cookie(viewerCookie(id))).status

        fun closesPreview() = browser.findElement(By.cssSelector("#customer-card .close")).click()

        fun seesPreview() = browser.findElements(By.cssSelector("#customer-card h3")).isNotEmpty()

        fun seesProviderOn(ticket: TicketId): String {
            visit(ticket)
            return browser.findElement(By.id("provider")).text
        }

        fun switchesProvider() = browser.findElement(By.id("provider")).click()

        override fun finds(ticket: TicketId): Boolean {
            visit(ticket)
            return browser.findElements(By.cssSelector("#header.gone")).isEmpty()
        }

        private fun visit(ticket: TicketId) {
            if (on == ticket) return
            browser.get("http://localhost/orgs/acme/t/$ticket")
            on = ticket
        }

        private fun button(ticket: TicketId, label: String): WebElement? {
            visit(ticket)
            return browser.findElements(By.cssSelector("#header .actions button")).firstOrNull { it.text == label }
        }
    }
}

internal val buttons = mapOf("Take it" to Action.Take, "Mark pending" to Action.Pend, "Resolve" to Action.Resolve, "Reopen" to Action.Reopen)
