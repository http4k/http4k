package org.http4k.fanbelt.load

import boxoffice.domain.Actor
import boxoffice.domain.Layout
import boxoffice.domain.OrderId
import boxoffice.domain.SeatRef
import boxoffice.domain.ShowId
import boxoffice.domain.ShowSeat
import boxoffice.domain.ShowTitle
import boxoffice.domain.ViewerId
import boxoffice.feature.BoxOfficeFeature
import boxoffice.store.BoxOfficeTables
import boxoffice.store.Theatres
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.http4k.format.value
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.exposed.ExposedDocumentStorage
import org.http4k.fanbelt.store.exposed.PostgresDocuments
import org.http4k.fanbelt.store.memory.InMemoryDocumentStorage
import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.fanbelt.withFanbeltMappings
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.time.Clock
import java.time.Duration
import java.util.Random
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.atomic.AtomicLong

object LoadBoxOfficeJson : ConfigurableMoshi(
    standardConfig().withFanbeltMappings().value(ShowId).value(SeatRef).value(ViewerId).value(ShowTitle).value(OrderId).done()
)

fun hotTenant(fans: Int = 20, duration: Duration = Duration.ofSeconds(10)): Report {
    val counts = Counts()
    val storage = CountingStorage(boxOfficeStorage(), counts)
    val executor = Executors.newVirtualThreadPerTaskExecutor()
    val theatre = TenantId.of("hot-${System.nanoTime()}")
    val layout = Layout(rows = 20, seatsPerRow = 30)
    Theatres(storage).create(theatre, Clock.systemUTC(), Random(), ShowTitle.of("Hot show") to layout)
    val scope = AppInfra(AppRuntime(), storage).apply { start() }.scope(theatre)
    val boxOffice = BoxOfficeFeature(Clock.systemUTC())
    val show = scope.read(boxOffice.shows).single().id
    val seats = layout.seats

    val holders = ConcurrentHashMap<SeatRef, ViewerId>()
    val reserved = AtomicLong()
    val taken = AtomicLong()
    val doubleBooked = AtomicLong()
    val failed = AtomicLong()
    val latencies = ConcurrentLinkedQueue<Long>()
    val until = System.nanoTime() + duration.toNanos()
    val before = counts.snapshot()

    repeat(fans) {
        executor.submit {
            val fan = ViewerId.of(UUID.randomUUID())
            val random = Random()
            while (System.nanoTime() < until) {
                val seat = seats[random.nextInt(seats.size)]
                val started = System.nanoTime()
                runCatching { scope.execute(boxOffice.reserve, Actor.Fan(fan), ShowSeat(show, seat)) }
                    .onSuccess { outcome ->
                        latencies += System.nanoTime() - started
                        if (outcome !is Changes) taken.incrementAndGet()
                        else {
                            reserved.incrementAndGet()
                            if ((holders.putIfAbsent(seat, fan) ?: fan) != fan) doubleBooked.incrementAndGet()
                        }
                    }
                    .onFailure { failed.incrementAndGet() }
            }
        }
    }
    executor.shutdown()
    executor.awaitTermination(duration.seconds + 30, SECONDS)

    val after = counts.snapshot() - before
    val stored = scope.read(boxOffice.bookings, show)
    check(failed.get() == 0L) { "${failed.get()} reservations failed" }
    check(doubleBooked.get() == 0L) { "${doubleBooked.get()} seats were reserved for two fans" }
    check(stored.size == holders.size) { "${holders.size} seats reserved, ${stored.size} stored" }

    return Report("hot-tenant", "$fans fans racing for ${seats.size} seats at one show for ${duration.seconds} s, ${if (POSTGRES == null) "memory" else "Postgres"} storage.")
        .row("Reservations accepted", reserved.get())
        .row("Reservations accepted per second", "%.1f".format(reserved.get().toDouble() / duration.seconds))
        .row("Reservations refused as taken", taken.get())
        .row("Reservations that failed", failed.get())
        .row("Seats reserved for two fans", doubleBooked.get())
        .row("Seats reserved", holders.size)
        .row("Reservation latency, p50", percentile(latencies.toList(), 0.5))
        .row("Reservation latency, p95", percentile(latencies.toList(), 0.95))
        .row("Reservation latency, max", percentile(latencies.toList(), 1.0))
        .row("Write transactions per reservation attempt", per(after.writes, (reserved.get() + taken.get()).toInt()))
        .write()
}

private fun boxOfficeStorage(): DocumentStorage {
    val url = POSTGRES ?: return InMemoryDocumentStorage()
    val db = Database.connect(postgres(url).dataSource)
    transaction(db) {
        SchemaUtils.create(*BoxOfficeTables.all.map(PostgresDocuments::schema).toTypedArray())
    }
    return ExposedDocumentStorage(db, LoadBoxOfficeJson, PostgresDocuments)
}
