package boxoffice.jobs

import boxoffice.domain.Actor
import boxoffice.domain.Order
import boxoffice.domain.OrderStatus
import boxoffice.feature.OrdersFeature
import boxoffice.store.BoxOfficeTables
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.PAYMENT_REQUIRED
import org.http4k.core.body.form
import org.http4k.events.Events
import org.http4k.fanbelt.events.FanbeltFailure
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.infrastructure.Lifecycle
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.eq
import org.http4k.fanbelt.tenancy.Scope
import org.http4k.fanbelt.tenancy.TenantId
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit.SECONDS

data class PaymentSweepFailed(override val cause: Throwable) : FanbeltFailure

class PaymentJobs(
    private val storage: DocumentStorage,
    private val scope: (TenantId) -> Scope,
    private val scheduler: ScheduledExecutorService,
    private val events: Events,
    private val orders: OrdersFeature,
    private val paymentProvider: HttpHandler,
) : Lifecycle {
    constructor(runtime: AppRuntime, infra: AppInfra, orders: OrdersFeature) :
        this(infra.storage, infra::scope, runtime.scheduler, runtime.events, orders, runtime.http)

    @Volatile
    private var sweeping: ScheduledFuture<*>? = null

    override fun start() {
        sweeping = scheduler.scheduleWithFixedDelay(::sweep, 1, 1, SECONDS)
    }

    override fun close() {
        sweeping?.cancel(false)
    }

    private fun sweep() {
        runCatching {
            storage.read { it.having(BoxOfficeTables.ORDERS, Order::status eq OrderStatus.Pending) }.forEach { charge(scope(it)) }
        }.onFailure { events(PaymentSweepFailed(it)) }
    }

    private fun charge(scope: Scope) = scope.read(orders) { it.pending() }.forEach { order -> take(scope, order) }

    private fun take(scope: Scope, order: Order) {
        val charged = paymentProvider(Request(POST, "/charge").form("order", order.id.toString()).form("seats", order.seats.size.toString()))
        when {
            charged.status.successful -> scope.execute(orders.paid, Actor.System, order.id)
            charged.status == PAYMENT_REQUIRED -> scope.execute(orders.declined, Actor.System, order.id)
        }
    }
}