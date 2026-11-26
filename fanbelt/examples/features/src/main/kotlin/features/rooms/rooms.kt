package features.rooms

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.length
import dev.forkhandles.values.regex
import features.ViewerId
import org.http4k.fanbelt.store.Identified
import java.util.UUID

class RoomName private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<RoomName>(::RoomName, "[a-z0-9-]{1,40}".regex)
}

class RoomTitle private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<RoomTitle, String>(::RoomTitle, (1..80).length, String::trim)
}

class LineId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<LineId>(::LineId)
}

class LineText private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<LineText, String>(::LineText, (1..280).length, String::trim)
}

class SeenId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<SeenId>(::SeenId) {
        fun of(reader: Reader) = of("${reader.room}:${reader.viewer}")
    }
}

data class Room(override val id: RoomName, val title: RoomTitle, val owner: ViewerId, val private: Boolean) : Identified<RoomName> {
    fun visibleTo(viewer: ViewerId) = !private || viewer == owner
}

data class Line(override val id: LineId, val room: RoomName, val by: ViewerId, val text: LineText) : Identified<LineId>

data class Seen(override val id: SeenId, val viewer: ViewerId, val room: RoomName, val count: Int) : Identified<SeenId>

data class Reader(val room: RoomName, val viewer: ViewerId)

data class NewRoom(val name: String, val private: Boolean)

data class Rename(val room: RoomName, val title: String)

data class Said(val room: RoomName, val text: String)
