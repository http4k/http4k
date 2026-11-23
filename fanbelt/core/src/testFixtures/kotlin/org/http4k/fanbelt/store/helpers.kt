package org.http4k.fanbelt.store

import dev.forkhandles.values.AbstractComparableValue
import dev.forkhandles.values.AbstractValue
import dev.forkhandles.values.InstantValueFactory
import dev.forkhandles.values.Maskers.hidden
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.http4k.format.value
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

class RowId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<RowId>(::RowId)
}

fun ids(vararg values: String) = values.map(RowId::of)

data class Note(override val id: RowId, val topic: String, val author: String?, val size: Int, val pinned: Boolean = false, val at: Instant? = null, val day: LocalDate? = null, val local: LocalDateTime? = null) : Identified<RowId>

val notes = TableName.of("notes")

data class Tag(val topic: String, val n: Int) {
    override fun toString() = "$topic-$n"
}

data class Tagged(override val id: RowId, val tag: Tag, val size: Int) : Identified<RowId>

val tagged = TableName.of("tagged")

class Stamp private constructor(value: Instant) : AbstractValue<Instant>(value) {
    companion object : InstantValueFactory<Stamp>(::Stamp)
}

class Moment private constructor(value: Instant) : AbstractComparableValue<Moment, Instant>(value) {
    companion object : InstantValueFactory<Moment>(::Moment)
}

data class Stamped(override val id: RowId, val stamp: Stamp?, val moment: Moment? = null) : Identified<RowId>

val stamped = TableName.of("stamped")

class Hushed private constructor(value: String) : StringValue(value, hidden()) {
    companion object : StringValueFactory<Hushed>(::Hushed)
}

data class Whisper(override val id: Hushed, val text: String) : Identified<Hushed>

val whispers = TableName.of("whispers")

object PagingJson : ConfigurableMoshi(standardConfig().value(Stamp).value(Moment).value(Hushed).value(RowId).done())
