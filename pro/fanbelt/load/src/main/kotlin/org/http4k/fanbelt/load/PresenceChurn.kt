package org.http4k.fanbelt.load

import org.http4k.fanbelt.state.set
import sheets.domain.COLUMNS
import sheets.domain.CellRef
import sheets.domain.ROWS
import sheets.domain.ViewerId
import sheets.feature.SheetFeature
import java.time.Duration
import java.util.Random
import java.util.UUID

fun presenceChurn(nodes: Int = 3, watchersPerNode: Int = 20, moves: Int = 600, perSecond: Int = 50): Report {
    val counts = Counts()
    val random = Random(1)
    SheetsCluster(nodes, counts).use { cluster ->
        val watchers = (0 until nodes).flatMap { node ->
            (1..watchersPerNode).map { Watcher(node, cluster.nodes[node].handler, ViewerId.of(UUID.randomUUID()), "/sheets/${cluster.tenant}/") }
        }
        pause(Duration.ofSeconds(2))
        val before = counts.snapshot()
        val patchesBefore = watchers.sumOf { it.patches.get() }
        val bytesBefore = watchers.sumOf { it.bytes.get() }
        val brotliBefore = watchers.sumOf { it.brotliBytes.get() }
        val brotliNanosBefore = watchers.sumOf { it.brotliNanos.get() }

        repeat(moves) {
            val mover = watchers[random.nextInt(watchers.size)]
            val cell = CellRef.of(COLUMNS[random.nextInt(COLUMNS.size)], random.nextInt(ROWS) + 1)
            SheetFeature.here.set(cluster.scope(mover.node), mover.viewer, cell)
            pause(Duration.ofMillis(1000L / perSecond))
        }
        pause(Duration.ofSeconds(3))

        val after = counts.snapshot() - before
        val patches = watchers.sumOf { it.patches.get() } - patchesBefore
        val bytes = watchers.sumOf { it.bytes.get() } - bytesBefore
        val brotli = watchers.sumOf { it.brotliBytes.get() } - brotliBefore
        val brotliNanos = watchers.sumOf { it.brotliNanos.get() } - brotliNanosBefore

        return Report("presence-churn", "$nodes nodes × $watchersPerNode watchers on one sheet, $moves cursor moves at $perSecond/s, ${mode()}.")
            .row("Presence writes per move", per(after.presenceWrites, moves))
            .row("Backplane messages per move", per(after.messages, moves))
            .row("Storage reads per move", per(after.reads, moves))
            .row("Renders per move", per(after.renders, moves))
            .row("Patches per move per watcher", per(patches, moves * watchers.size))
            .row("Bytes per patch", per(bytes, patches.toInt()))
            .row("Brotli bytes per patch", per(brotli, patches.toInt()))
            .row("Brotli ratio (bytes ÷ brotli bytes)", per(bytes, brotli.toInt()))
            .row("Brotli time per patch", "${per(brotliNanos / 1000, patches.toInt())} µs")
            .write()
    }
}
