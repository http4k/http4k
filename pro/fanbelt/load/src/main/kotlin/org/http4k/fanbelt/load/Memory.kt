package org.http4k.fanbelt.load

import org.http4k.core.ContentEncoding
import org.http4k.core.patched.Brotli
import org.http4k.fanbelt.state.set
import sheets.domain.COLUMNS
import sheets.domain.CellRef
import sheets.domain.ROWS
import sheets.domain.ViewerId
import sheets.feature.SheetFeature
import java.io.OutputStream
import java.time.Duration
import java.util.Random
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

fun memory(connections: Int = 100, windows: List<Int> = listOf(16, 18, 20, 22)): Report {
    ContentEncoding.Brotli()
    val patches = presencePatches(moves = 300).map(String::toByteArray)
    val report = Report(
        "memory",
        "$connections SSE connections, each sent the same ${patches.size} presence patches (${patches.sumOf { it.size } / 1024} KB) " +
            "through its own brotli encoder at quality 4, flushed per patch. After a warm-up round, encoders stay open, so each window adds to the last. " +
            "brotli4j leaves the window to brotli, whose default is 2^22, ${mode()}."
    )
    compress(patches, ByteCounter(), connections, windows.first()).forEach(OutputStream::close)
    val open = windows.flatMap { window ->
        settle()
        val before = residentBytes()
        val counter = ByteCounter()
        val encoders = compress(patches, counter, connections, window)
        settle()
        val grown = residentBytes() - before
        report
            .row("Window 2^$window (${(1 shl window) / 1024} KB): resident memory per connection", "${grown / connections / 1024} KB")
            .row("Window 2^$window (${(1 shl window) / 1024} KB): brotli bytes per patch", per(counter.count, patches.size * connections))
        encoders
    }
    open.forEach(OutputStream::close)
    return report.write()
}

private fun compress(patches: List<ByteArray>, counter: ByteCounter, connections: Int, window: Int): List<OutputStream> {
    val encoders = (1..connections).map { ContentEncoding.Brotli(window = window).encoder(counter) }
    patches.forEach { patch ->
        encoders.forEach {
            it.write(patch)
            it.flush()
        }
    }
    return encoders
}

private fun settle() {
    System.gc()
    pause(Duration.ofSeconds(1))
}

private fun presencePatches(moves: Int): List<String> {
    val counts = Counts()
    val random = Random(1)
    val patches = CopyOnWriteArrayList<String>()
    SheetsCluster(1, counts).use { cluster ->
        val watchers = (1..60).map { index ->
            Watcher(0, cluster.nodes[0].handler, ViewerId.of(UUID.randomUUID()), "/sheets/${cluster.tenant}/", { patch -> if (index == 1) patches += patch })
        }
        pause(Duration.ofSeconds(2))
        repeat(moves) {
            val mover = watchers[random.nextInt(watchers.size)]
            SheetFeature.here.set(cluster.scope(0), mover.viewer, CellRef.of(COLUMNS[random.nextInt(COLUMNS.size)], random.nextInt(ROWS) + 1))
            pause(Duration.ofMillis(20))
        }
        pause(Duration.ofSeconds(1))
    }
    return patches.toList()
}

private fun residentBytes() =
    ProcessBuilder("ps", "-o", "rss=", "-p", ProcessHandle.current().pid().toString()).start()
        .inputStream.bufferedReader().readText().trim().toLong() * 1024

private class ByteCounter : OutputStream() {
    var count = 0L

    override fun write(b: Int) {
        count++
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        count += len
    }
}
