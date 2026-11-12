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
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.Access.Allowed
import org.http4k.fanbelt.feature.Access.Denied
import org.http4k.fanbelt.feature.Access.Unavailable
import org.http4k.fanbelt.feature.Outcome.Rejected
import java.time.Clock

class BoxOfficeFeature(clock: Clock) : Feature<BoxOffice, Actor>({ tables -> BoxOffice(BoxOfficeTables(tables), clock) }) {
    val shows by state { boxOffice -> boxOffice.shows() }
    val bookings by state(ShowId) { boxOffice, id -> boxOffice.bookings(id) }
    val watching by presence(ShowId, ViewerId)

    val seatMap by derived { ctx, id: ShowId ->
        val live = ctx.domain.live(ctx.read(bookings, id))
        live.filter { it.order == null }.forEach { ctx.staleAt(it.expiresAt) }
        val holders = live.associate { it.id.seat to it.fan }
        val show = ctx.domain.show(id)
        if (show == null) null else SeatMap(show, show.layout.seats.map { seat -> SeatView(seat, holders[seat]) })
    }

    val header by derived { ctx, id: ShowId ->
        val map = ctx.read(seatMap, id)
        if (map == null) null else Header(map.show.title, map.seats.count { it.reservedBy == null }, ctx.read(watching, id).keys)
    }

    val fan by rule { _, actor -> if (actor is Actor.Fan) Allowed(actor) else Denied }

    val yourBooking by rule { ctx, actor, pick: ShowSeat ->
        val booking = ctx.domain.booking(pick)
        when {
            actor !is Actor.Fan -> Denied
            booking == null -> Unavailable(seatField, "Your reservation of ${pick.seat} has lapsed")
            booking.fan != actor.id -> Denied
            else -> Allowed(booking)
        }
    }

    val reserve by operation(fan) { boxOffice, _, pick: ShowSeat, actor ->
        val show = boxOffice.show(pick.show) ?: return@operation Rejected(seatField, "No such show")
        if (pick.seat !in show.layout) return@operation Rejected(seatField, "No such seat at this show")
        val reservedBy = boxOffice.booking(pick)?.fan
        if (reservedBy != null && reservedBy != actor.id) return@operation Rejected(seatField, "Someone just took ${pick.seat}")
        boxOffice.reserve(pick, actor.id)
        Changes.of(bookings.topic(pick.show))
    }

    val release by operation(yourBooking) { boxOffice, _, pick, booking ->
        if (booking.order != null) return@operation Rejected(seatField, "${pick.seat} is already paid for")
        boxOffice.release(booking)
        Changes.of(bookings.topic(pick.show))
    }
}