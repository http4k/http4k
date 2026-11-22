package boxoffice.domain

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.length
import java.time.Duration
import java.time.Instant
import java.util.UUID
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.store.Identified

val RESERVED_FOR: Duration = Duration.ofMinutes(5)

class SeatRef private constructor(value: String) : StringValue(value) {
    val row get() = value.first()
    val number get() = value.drop(1).toInt()

    companion object : StringValueFactory<SeatRef>(::SeatRef, { it.matches(Regex("[A-Z][0-9]{1,2}")) })
}

val seatOrder = compareBy<SeatRef>({ it.row }, { it.number })

val seatField = FieldName.of("seat")

data class Layout(val rows: Int, val seatsPerRow: Int) {
    val seats get() = ('A'..'Z').take(rows).flatMap { row -> (1..seatsPerRow).map { SeatRef.of("$row$it") } }

    operator fun contains(seat: SeatRef) = seat.row < 'A' + rows && seat.number in 1..seatsPerRow
}

class ShowId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<ShowId>(::ShowId)
}

class ShowTitle private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<ShowTitle, String>(::ShowTitle, (1..80).length.and { it.isNotBlank() }, String::trim)
}

data class Show(override val id: ShowId, val title: ShowTitle, val layout: Layout) : Identified<ShowId>

class ShowSeat private constructor(value: String) : StringValue(value) {
    val show get() = ShowId.parse(value.substringBefore(':'))
    val seat get() = SeatRef.of(value.substringAfter(':'))

    companion object : StringValueFactory<ShowSeat>(::ShowSeat, { it.matches(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}:[A-Z][0-9]{1,2}")) })
}

fun ShowSeat(show: ShowId, seat: SeatRef) = ShowSeat.of("$show:$seat")

data class Booking(override val id: ShowSeat, val fan: ViewerId, val expiresAt: Instant, val order: OrderId? = null, val show: ShowId = id.show) : Identified<ShowSeat> {
    fun holdsSeatAt(now: Instant) = order != null || now < expiresAt
}

class TheatreId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<TheatreId>(::TheatreId)
}

data class Theatre(val createdAt: Instant, override val id: TheatreId = SELF) : Identified<TheatreId> {
    companion object {
        val SELF = TheatreId.of("self")
    }
}

data class SeatView(val seat: SeatRef, val reservedBy: ViewerId?)

data class SeatMap(val show: Show, val seats: List<SeatView>)

data class Header(val title: ShowTitle, val seatsLeft: Int, val watching: Set<ViewerId>)

sealed interface Actor {
    data class Fan(val id: ViewerId) : Actor
    data object System : Actor
}