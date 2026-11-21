/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

import dev.forkhandles.values.AbstractComparableValue
import dev.forkhandles.values.AbstractValue
import dev.forkhandles.values.InstantValueFactory
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.standardConfig
import org.http4k.format.value
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

data class Note(override val id: String, val topic: String, val author: String?, val size: Int, val pinned: Boolean = false, val at: Instant? = null, val day: LocalDate? = null, val local: LocalDateTime? = null) : Identified<String>

val notes = TableName.of("notes")

data class Tag(val topic: String, val n: Int) {
    override fun toString() = "$topic-$n"
}

data class Tagged(override val id: Tag, val size: Int) : Identified<Tag>

val tagged = TableName.of("tagged")

class Stamp private constructor(value: Instant) : AbstractValue<Instant>(value) {
    companion object : InstantValueFactory<Stamp>(::Stamp)
}

class Moment private constructor(value: Instant) : AbstractComparableValue<Moment, Instant>(value) {
    companion object : InstantValueFactory<Moment>(::Moment)
}

data class Stamped(override val id: String, val stamp: Stamp?, val moment: Moment? = null) : Identified<String>

val stamped = TableName.of("stamped")

object PagingJson : ConfigurableMoshi(standardConfig().value(Stamp).value(Moment).done())
