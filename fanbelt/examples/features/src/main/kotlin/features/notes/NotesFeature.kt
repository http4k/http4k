package features.notes

import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import dev.forkhandles.values.ValueFactory
import dev.forkhandles.values.length
import dev.forkhandles.values.random
import dev.forkhandles.values.StringValue
import features.ViewerId
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.check
import org.http4k.fanbelt.feature.orElse
import org.http4k.fanbelt.input.FieldName
import org.http4k.fanbelt.state.topic
import org.http4k.fanbelt.store.Identified
import org.http4k.fanbelt.store.Order.Newest
import org.http4k.fanbelt.store.TableName
import org.http4k.fanbelt.store.Tables
import org.http4k.fanbelt.store.invoke
import java.util.Random
import java.util.UUID

class NoteId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<NoteId>(::NoteId)
}

class NoteText private constructor(value: String) : StringValue(value) {
    companion object : ValueFactory<NoteText, String>(::NoteText, (1..280).length, String::trim)
}

class Shown private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<Shown>(::Shown, { it in 1..1000 })
}

data class Note(override val id: NoteId, val by: ViewerId?, val text: NoteText) : Identified<NoteId>

data class Window(val notes: List<Note>, val more: Boolean)

class Noticeboard(tables: Tables, private val random: Random) {
    private val notes = tables<NoteId, Note>(NOTES)

    fun count() = notes.count()

    fun newest(shown: Shown) = notes.page(order = Newest, limit = shown.value + 1).items
        .let { Window(it.take(shown.value), it.size > shown.value) }

    fun post(by: ViewerId, text: NoteText) = notes.insert(Note(NoteId.random(random), by, text))

    fun announce(text: String) {
        val id = NoteId.of(UUID.nameUUIDFromBytes(text.toByteArray()))
        if (notes.get(id) == null) notes.insert(Note(id, null, NoteText.of(text)))
    }

    companion object {
        val NOTES = TableName.of("notes")
    }
}

class NotesFeature(random: Random) : Feature<Noticeboard, ViewerId>({ tables -> Noticeboard(tables, random) }) {
    val count by state { it.count() }

    val newest by derived { ctx, shown: Shown ->
        ctx.watch(count.topic())
        ctx.domain.newest(shown)
    }

    val post by operation { noticeboard, by, raw: String ->
        val text = FieldName.of("note").check(NoteText, raw, "A note is 1 to 280 characters").orElse { return@operation it }
        noticeboard.post(by, text)
        Changes.of(count.topic())
    }
}
