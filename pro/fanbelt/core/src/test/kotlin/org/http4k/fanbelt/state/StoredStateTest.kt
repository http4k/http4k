/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.state

import dev.forkhandles.time.DeterministicScheduler
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.fanbelt.acme
import org.http4k.fanbelt.feature.Changes
import org.http4k.fanbelt.feature.Feature
import org.http4k.fanbelt.feature.execute
import org.http4k.fanbelt.infrastructure.AppInfra
import org.http4k.fanbelt.infrastructure.AppRuntime
import org.http4k.fanbelt.page.Region
import org.http4k.template.ViewModel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private val names = ConcurrentHashMap(mapOf("b1" to "Plans", "b2" to "Ideas"))

class StoredStateTest {
    private class BoardId private constructor(value: String) : StringValue(value) {
        companion object : NonBlankStringValueFactory<BoardId>(::BoardId)
    }

    private data class Shown(val name: String) : ViewModel

    private object Boards : Feature<MutableMap<String, String>, Unit>({ names }) {
        val board by state(BoardId) { names, id -> names.getValue(id.value) }
        val rename by operation { names, _, change: Pair<BoardId, String> ->
            names[change.first.value] = change.second
            Changes.of(board.topic(change.first))
        }
    }

    private val scheduler = DeterministicScheduler(Instant.EPOCH)
    private val scope = AppInfra(AppRuntime(scheduler = scheduler)).apply { start() }.scope(acme)

    @Test
    fun `a region over a stored state is redrawn when an operation changes it, and not for another key`() {
        val shown = mutableListOf<String>()
        val region = Region("board", { """<h1 id="board">${(it as Shown).name}</h1>""" }, { _: Request -> BoardId.of("b1") }, Boards.board) { name, _ -> Shown(name) }
        region.watch(scope, Request(GET, "/")) { shown += it.html }

        scope.execute(Boards.rename, Unit, BoardId.of("b2") to "Someday")
        scheduler.runUntilIdle()
        scope.execute(Boards.rename, Unit, BoardId.of("b1") to "Roadmap")
        scheduler.runUntilIdle()

        assertEquals(listOf("""<h1 id="board">Plans</h1>""", """<h1 id="board">Roadmap</h1>"""), shown)
    }
}
