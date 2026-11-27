package features.rooms

import features.notes.NotesFeature
import features.notes.Noticeboard
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.state.topic
import java.util.Random

data object Announcer

class AnnouncementsFeature(notes: NotesFeature, random: Random) : Feature<Noticeboard, Announcer>({ tables -> Noticeboard(tables, random) }) {
    val announce by operation { board, _, room: RoomName ->
        board.announce("Room $room opened")
        Changes.of(notes.count.topic())
    }

    val opened by job(RoomName) { scope, room -> scope.execute(announce, Announcer, room) }
}
