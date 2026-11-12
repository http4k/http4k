package org.http4k.fanbelt.load

import org.http4k.fanbelt.feature.execute
import sheets.domain.COLUMNS
import sheets.domain.CellRef
import sheets.domain.ROWS
import sheets.domain.ViewerId
import sheets.feature.SheetFeature
import java.time.Duration
import java.util.UUID

fun fanout(nodes: Int = 3, watchersPerNode: Int = 20, changes: Int = COLUMNS.size * ROWS, perSecond: Int = 50): Report {
    val counts = Counts()
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
        val writer = ViewerId.of(UUID.randomUUID())
        val cells = COLUMNS.flatMap { column -> (1..ROWS).map { row -> CellRef.of(column, row) } }

        val sent = (0 until changes).map { change ->
            val node = change % nodes
            val started = System.nanoTime()
            cluster.scope(node).execute(SheetFeature.set, writer, cells[change % cells.size] to "v$change")
            pause(Duration.ofMillis(1000L / perSecond))
            Triple("v$change", node, started)
        }
        pause(Duration.ofSeconds(3))

        val after = counts.snapshot() - before
        val elsewhere = sent.flatMap { (marker, node, started) ->
            watchers.filter { it.node != node }.map { watcher -> watcher.sawAt(marker)?.minus(started) }
        }
        val latencies = elsewhere.filterNotNull()
        val patches = watchers.sumOf { it.patches.get() } - patchesBefore
        val bytes = watchers.sumOf { it.bytes.get() } - bytesBefore
        val brotli = watchers.sumOf { it.brotliBytes.get() } - brotliBefore
        val brotliNanos = watchers.sumOf { it.brotliNanos.get() } - brotliNanosBefore

        return Report("fanout", "$nodes nodes × $watchersPerNode watchers on one sheet, $changes cell changes at $perSecond/s, ${mode()}.")
            .row("Command to patch on another node, p50", percentile(latencies, 0.5))
            .row("Command to patch on another node, p95", percentile(latencies, 0.95))
            .row("Command to patch on another node, max", percentile(latencies, 1.0))
            .row("Changes some watcher never saw (coalesced or lost)", "${elsewhere.size - latencies.size} of ${elsewhere.size}")
            .row("Backplane messages per change", per(after.messages, changes))
            .row("Storage reads per change", per(after.reads, changes))
            .row("Storage writes per change", per(after.writes, changes))
            .row("Renders per change", per(after.renders, changes))
            .row("Renders per change per watcher (1.00 = no sharing)", per(after.renders, changes * watchers.size))
            .row("Patches per change per watcher", per(patches, changes * watchers.size))
            .row("Bytes per patch", per(bytes, patches.toInt()))
            .row("Brotli bytes per patch", per(brotli, patches.toInt()))
            .row("Brotli ratio (bytes ÷ brotli bytes)", per(bytes, brotli.toInt()))
            .row("Brotli time per patch", "${per(brotliNanos / 1000, patches.toInt())} µs")
            .write()
    }
}
