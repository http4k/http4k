/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import org.http4k.format.Moshi
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal object ClassIndexWriter {

    private val locks = ConcurrentHashMap<String, Any>()

    fun rebuild(classDir: File, className: String) {
        val lock = locks.computeIfAbsent(classDir.absolutePath) { Any() }
        synchronized(lock) {
            val entries = classDir.listFiles { f -> f.isFile && f.name.endsWith(".html") && f.name != "index.html" }
                ?.sortedBy { it.name }
                ?.map { html -> entryFor(html) }
                ?: emptyList()

            File(classDir, "index.html").writeText(
                storyboardRenderer(
                    StoryboardIndexView(
                        pageTitle = "Storyboards: $className",
                        heading = className,
                        tests = entries
                    )
                )
            )
        }
    }

    private fun entryFor(html: File): IndexEntryView {
        val baseName = html.nameWithoutExtension
        val json = File(html.parentFile, "$baseName.json")
        val story = json.takeIf { it.isFile }?.runCatching { Moshi.asA<Story>(readText()) }?.getOrNull()
        return IndexEntryView(
            name = baseName,
            href = html.name,
            outcome = story?.outcome?.name?.lowercase() ?: "unknown",
            duration = story?.durationMs?.let(::formatDuration) ?: ""
        )
    }
}

private fun formatDuration(ms: Long): String = when {
    ms < 1000 -> "${ms}ms"
    ms < 60_000 -> "%.1fs".format(ms / 1000.0)
    else -> "${ms / 60_000}m ${(ms % 60_000) / 1000}s"
}

internal data class StoryboardIndexView(
    val pageTitle: String,
    val heading: String,
    val tests: List<IndexEntryView>
) : StoryboardViewModel()

internal data class IndexEntryView(val name: String, val href: String, val outcome: String, val duration: String)
