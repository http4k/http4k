package boxoffice.feature

import boxoffice.domain.Actor
import boxoffice.domain.Header
import boxoffice.domain.SeatMap
import boxoffice.domain.SeatView
import boxoffice.domain.ShowId
import boxoffice.domain.ShowSeat
import boxoffice.domain.ViewerId
import boxoffice.domain.seatField
import boxoffice.store.BoxOfficeTables
import java.time.Clock
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.feature.flatMap

class BoxOfficeFeature(clock: Clock) : Feature<BoxOffice, Actor>({ tables -> BoxOffice(BoxOfficeTables(tables), clock) }) {
    val shows by state { boxOffice -> boxOffice.shows() }
    val bookings by state(ShowId) { boxOffice, id -> boxOffice.bookings(id) }
    val watching by presence(ShowId, ViewerId)

    val seatMap by derived { ctx, id: ShowId ->
        val held = ctx.domain.stillHeld(ctx.read(bookings, id))
        held.filter { it.order == null }.forEach { ctx.stale(at = it.expiresAt) }
        val holders = held.associate { it.id.seat to it.fan }
        val show = ctx.domain.show(id)
        if (show == null) null else SeatMap(show, show.layout.seats.map { seat -> SeatView(seat, holders[seat]) })
    }

    val header by derived { ctx, id: ShowId ->
        val map = ctx.read(seatMap, id)
        if (map == null) null else Header(map.show.title, map.seats.count { it.reservedBy == null }, ctx.read(watching, id).keys)
    }

    val fan by rule { _, actor -> if (actor is Actor.Fan) Allowed(actor) else Denied }

    val watch by visit(fan) { scope, _, show: ShowId, fan -> scope.join(watching, fan.id, show) }

    val yourBooking by rule { ctx, actor, pick: ShowSeat ->
        ctx.read(fan, actor).flatMap { fan ->
            val booking = ctx.domain.booking(pick)
            when {
                booking == null -> Rejected(seatField, "You have no reservation of ${pick.seat}")
                booking.fan != fan.id -> Denied
                else -> Allowed(booking)
            }
        }
    }

    val reserve by operation(fan) { boxOffice, actor, pick: ShowSeat, fan ->
        val show = boxOffice.show(pick.show) ?: return@operation Rejected(seatField, "No such show")
        if (pick.seat !in show.layout) return@operation Rejected(seatField, "No such seat at this show")
        val reservedBy = boxOffice.booking(pick)?.fan
        if (reservedBy != null && reservedBy != fan.id) return@operation Rejected(seatField, "Someone just took ${pick.seat}")
        boxOffice.reserve(pick, fan.id)
        Changes.of(bookings.topic(pick.show))
    }

    val release by operation(yourBooking) { boxOffice, actor, pick: ShowSeat, booking ->
        if (booking.order != null) return@operation Rejected(seatField, "${pick.seat} is already paid for")
        boxOffice.release(booking)
        Changes.of(bookings.topic(pick.show))
    }
}
