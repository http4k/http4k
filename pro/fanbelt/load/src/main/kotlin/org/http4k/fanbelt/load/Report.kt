package org.http4k.fanbelt.load

import java.io.File
import java.time.Duration

class Report(private val name: String, private val setup: String) {
    private val rows = mutableListOf<Pair<String, String>>()

    fun row(metric: String, value: Any) = apply { rows += metric to value.toString() }

    fun markdown() = buildString {
        appendLine("## $name")
        appendLine()
        appendLine(setup)
        appendLine()
        appendLine("| Metric | Value |")
        appendLine("|---|---|")
        rows.forEach { (metric, value) -> appendLine("| $metric | $value |") }
    }

    fun write(): Report {
        val file = File("results", "$name.md")
        file.parentFile.mkdirs()
        file.writeText(markdown())
        println(markdown())
        return this
    }
}

fun percentile(values: List<Long>, p: Double): String {
    if (values.isEmpty()) return "n/a"
    val sorted = values.sorted()
    return millis(sorted[((sorted.size - 1) * p).toInt()])
}

fun millis(nanos: Long) = "%.1f ms".format(nanos / 1_000_000.0)

fun per(count: Long, of: Int) = if (of == 0) "n/a" else "%.2f".format(count.toDouble() / of)

fun pause(duration: Duration) = Thread.sleep(duration.toMillis())
