package helpdesk.jobs

import helpdesk.domain.Actor
import helpdesk.domain.Failure
import helpdesk.domain.Outbox
import helpdesk.domain.OutboxStatus
import helpdesk.domain.TicketId
import helpdesk.feature.TicketsFeature
import helpdesk.store.HelpdeskTables
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.body.form
import org.http4k.events.Events
import org.http4k.fanbelt.events.FanbeltFailure
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Outcome
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.infrastructure.Lifecycle
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.eq
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.fanbelt.tenancy.TenantId
import java.time.Clock
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.SECONDS

data class NotifySweepFailed(override val cause: Throwable) : FanbeltFailure

class HelpdeskJobs(
    private val storage: DocumentStorage,
    private val scope: (TenantId) -> Scope,
    private val scheduler: ScheduledExecutorService,
    private val clock: Clock,
    private val events: Events,
    private val tickets: TicketsFeature,
    private val provider: HttpHandler,
) : Lifecycle {
    constructor(runtime: AppRuntime, infra: AppInfra, tickets: TicketsFeature) :
        this(infra.storage, infra::scope, runtime.scheduler, runtime.clock, runtime.events, tickets, runtime.http)

    @Volatile
    private var sweeping: ScheduledFuture<*>? = null

    override fun start() {
        sweeping = scheduler.scheduleWithFixedDelay(::sweep, 0, 1, SECONDS)
    }

    override fun close() {
        sweeping?.cancel(false)
    }

    private fun sweep() {
        runCatching {
            storage.read { it.having(HelpdeskTables.OUTBOX, Outbox::status eq OutboxStatus.Queued) }
                .forEach { notify(scope(it)) }
        }.onFailure { events(NotifySweepFailed(it)) }
    }

    private fun notify(scope: Scope) {
        scope.read(tickets) { it.dueNotifications(clock.instant()) }.forEach { id ->
            if (claimed(scope.execute(tickets.claimNotification, Actor.System, id to clock.instant()))) send(scope, id)
        }
    }

    private fun claimed(outcome: Outcome<Boolean>) = when (outcome) {
        is Changes -> outcome.result
        else -> false
    }

    private fun send(scope: Scope, id: TicketId) {
        val ticket = scope.read(tickets) { it.ticket(id) } ?: return
        val sent = provider(Request(POST, "/send").form("to", ticket.customer.value).form("subject", "Resolved: ${ticket.subject}"))
        if (sent.status.successful) scope.execute(tickets.notified, Actor.System, id)
        else scope.execute(tickets.notifyFailed, Actor.System, Failure(id, sent.status.toString(), clock.instant()))
    }
}