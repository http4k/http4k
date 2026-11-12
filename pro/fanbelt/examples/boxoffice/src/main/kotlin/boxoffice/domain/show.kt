package boxoffice.domain

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.length
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.store.Identified
import java.time.Duration
import java.time.Instant
import java.util.UUID

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

data class ShowSeat(val show: ShowId, val seat: SeatRef) {
    override fun toString() = "$show:$seat"
}

data class Booking(override val id: ShowSeat, val fan: ViewerId, val expiresAt: Instant, val order: OrderId? = null) : Identified<ShowSeat> {
    fun liveAt(now: Instant) = order != null || now < expiresAt
}

data class Theatre(val createdAt: Instant, override val id: String = SELF) : Identified<String> {
    companion object {
        const val SELF = "self"
    }
}

data class SeatView(val seat: SeatRef, val reservedBy: ViewerId?)

data class SeatMap(val show: Show, val seats: List<SeatView>)

data class Header(val title: ShowTitle, val seatsLeft: Int, val watching: Set<ViewerId>)

sealed interface Actor {
    data class Fan(val id: ViewerId) : Actor
    data object System : Actor
}