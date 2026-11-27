package features.rooms

import dev.forkhandles.values.random
import features.ViewerId
import org.http4k.fanbelt.feature.Allowed
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.feature.check
import org.http4k.fanbelt.feature.flatMap
import org.http4k.fanbelt.feature.orElse
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.state.topic
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.eq
import org.http4k.fanbelt.store.invoke
import java.util.Random

class Rooms(tables: Tables, private val random: Random) {
    private val rooms = tables<RoomName, Room>(ROOMS)
    private val lines = tables<LineId, Line>(LINES)
    private val seen = tables<SeenId, Seen>(SEEN)

    fun all() = rooms.page().items

    fun room(name: RoomName) = rooms.get(name)

    fun create(room: Room) = rooms.insert(room)

    fun rename(name: RoomName, title: RoomTitle) = rooms.update(name) { it.copy(title = title) }

    fun lines(room: RoomName) = lines.page(Line::room eq room).items

    fun say(room: RoomName, by: ViewerId, text: LineText) = lines.insert(Line(LineId.random(random), room, by, text))

    fun seen(viewer: ViewerId) = seen.page(Seen::viewer eq viewer).items.associate { it.room to it.count }

    fun markSeen(reader: Reader) {
        val id = SeenId.of(reader)
        val count = lines.count(Line::room eq reader.room)
        if (seen.get(id) == null) seen.insert(Seen(id, reader.viewer, reader.room, count)) else seen.update(id) { it.copy(count = count) }
    }

    companion object {
        val ROOMS = TableName.of("rooms")
        val LINES = TableName.of("lines")
        val SEEN = TableName.of("seen")
    }
}

private val nameField = FieldName.of("name")
private val titleField = FieldName.of("title")
private val lineField = FieldName.of("line")

class RoomsFeature(announcements: AnnouncementsFeature, random: Random) : Feature<Rooms, ViewerId>({ tables -> Rooms(tables, random) }) {
    val rooms by state { it.all() }
    val room by state(RoomName) { rooms, name -> rooms.room(name) }
    val lines by state(RoomName) { rooms, name -> rooms.lines(name) }
    val seen by state(ViewerId) { rooms, viewer -> rooms.seen(viewer) }
    val here by presence(ViewerId, RoomName)

    val unseen by derived { ctx, reader: Reader ->
        ctx.read(lines, reader.room).size - (ctx.read(seen, reader.viewer)[reader.room] ?: 0)
    }

    val visibleRoom by rule { ctx, viewer, name: RoomName ->
        val room = ctx.read(room, name)
        if (room?.visibleTo(viewer) == true) Allowed(room) else Hidden
    }

    val yourRoom by rule { ctx, viewer, name: RoomName ->
        ctx.read(visibleRoom, viewer, name).flatMap { room -> if (room.owner == viewer) Allowed(room) else Denied }
    }

    val create by operation { rooms, viewer, new: NewRoom ->
        val name = nameField.check(RoomName, new.name.trim(), "Use lowercase letters, numbers and dashes").orElse { return@operation it }
        if (rooms.room(name) != null) return@operation Rejected(nameField, "That name is taken")
        rooms.create(Room(name, RoomTitle.of(name.value), viewer, new.private))
        Changes.of(setOf(this.rooms.topic(), room.topic(name), announcements.opened.topic(name)), name)
    }

    val rename by operation(yourRoom, Rename::room) { rooms, _, rename, _ ->
        val title = titleField.check(RoomTitle, rename.title, "A title is 1 to 80 characters").orElse { return@operation it }
        rooms.rename(rename.room, title)
        Changes.of(this.rooms.topic(), room.topic(rename.room))
    }

    val say by operation(visibleRoom, Said::room) { rooms, viewer, said, _ ->
        val text = lineField.check(LineText, said.text, "Say something first").orElse { return@operation it }
        rooms.say(said.room, viewer, text)
        Changes.of(lines.topic(said.room))
    }

    val markSeen by operation(visibleRoom) { rooms, viewer, name: RoomName, _ ->
        rooms.markSeen(Reader(name, viewer))
        Changes.of(seen.topic(viewer))
    }

    val enter by visit { scope, viewer, name: RoomName ->
        scope.execute(markSeen, viewer, name)
        val lease = scope.join(here, viewer, name)
        AutoCloseable {
            scope.execute(markSeen, viewer, name)
            lease.close()
        }
    }
}
