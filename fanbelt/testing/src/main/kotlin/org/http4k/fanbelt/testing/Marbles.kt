/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.fanbelt.events.Executed
import org.http4k.fanbelt.events.FanbeltTrace
import org.http4k.fanbelt.events.Patched
import org.http4k.fanbelt.events.Published
import org.http4k.fanbelt.events.Redrawn
import org.http4k.fanbelt.events.eventTenant
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Outcome.Denied
import org.http4k.fanbelt.feature.Outcome.Hidden
import org.http4k.fanbelt.feature.Outcome.Rejected
import org.http4k.fanbelt.testing.Marble.Kind

data class Marble(val column: Int, val kind: Kind) {
    enum class Kind(val glyph: Char, val labelled: Boolean) {
        Changes('✓', true),
        Rejected('✗', true),
        Denied('⊘', true),
        Happened('●', false),
        Dropped('○', false),
    }
}

data class Lane(val name: String, val marbles: List<Marble>)

data class Timeline(val lanes: List<Lane>, val columns: Int) {
    val labelWidth = lanes.maxOfOrNull { it.name.length } ?: 0

    companion object {
        fun of(events: List<Event>, aliases: Map<String, String> = emptyMap()): Timeline {
            val traces = events.mapNotNull { event -> ((if (event is MetadataEvent) event.event else event) as? FanbeltTrace)?.to(event.tenant) }
            val longestFirst = aliases.entries.sortedByDescending { it.key.length }
            val lanes = traces.withIndex()
                .sortedBy { (_, traced) -> traced.first.stage }
                .groupBy(
                    { (_, traced) -> longestFirst.fold(traced.first.lane + traced.second) { name, (from, to) -> name.replace(from, to) } },
                    { (column, traced) -> Marble(column, traced.first.kind) },
                )
                .map { (name, marbles) -> Lane(name, marbles) }
            return Timeline(lanes, traces.size)
        }
    }
}

private val Event.tenant: String
    get() {
        val tenant = eventTenant(this) ?: return ""
        return " @$tenant"
    }

private val FanbeltTrace.stage
    get() = when (this) {
        is Executed -> 0
        is Published -> 1
        is Redrawn -> 2
        is Patched -> 3
    }

private val FanbeltTrace.lane
    get() = when (this) {
        is Executed -> "$actor $operation"
        is Published -> topic.value
        is Redrawn -> if (key.isEmpty()) "#$region" else "#$region($key)"
        is Patched -> "${actor ?: "browser"} #$region"
    }

private val FanbeltTrace.kind
    get() = when (this) {
        is Executed -> when (outcome) {
            is Changes -> Kind.Changes
            is Rejected -> Kind.Rejected
            Denied, Hidden -> Kind.Denied
        }

        is Published -> Kind.Happened

        is Redrawn -> if (changed) Kind.Happened else Kind.Dropped

        is Patched -> if (sent) Kind.Happened else Kind.Dropped
    }
