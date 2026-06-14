/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard

import freemarker.core.HTMLOutputFormat
import freemarker.template.Configuration
import freemarker.template.Configuration.VERSION_2_3_34
import freemarker.template.TemplateExceptionHandler
import org.http4k.format.Moshi
import org.http4k.template.FreemarkerTemplates
import org.http4k.template.ViewModel
import java.io.File
import java.util.concurrent.ConcurrentHashMap

internal object ClassIndexWriter {

    private val renderer = FreemarkerTemplates(Configuration(VERSION_2_3_34).apply {
        outputFormat = HTMLOutputFormat.INSTANCE
        templateExceptionHandler = TemplateExceptionHandler.IGNORE_HANDLER
    }).CachingClasspath()

    private val locks = ConcurrentHashMap<String, Any>()

    fun rebuild(classDir: File, className: String) {
        val lock = locks.computeIfAbsent(classDir.canonicalPath) { Any() }
        synchronized(lock) {
            val entries = classDir.listFiles { f -> f.isFile && f.name.endsWith(".html") && f.name != "index.html" }
                ?.sortedBy { it.name }
                ?.map { html -> entryFor(html) }
                ?: emptyList()

            File(classDir, "index.html").writeText(
                renderer(StoryboardIndexView(title = className, tests = entries))
            )
        }
    }

    private fun entryFor(html: File): IndexEntryView {
        val baseName = html.nameWithoutExtension
        val json = File(html.parentFile, "$baseName.json")
        val frameCount = json.takeIf { it.isFile }
            ?.runCatching { Moshi.asA<Story>(readText()).frames.size }
            ?.getOrNull()
            ?: 0
        return IndexEntryView(name = baseName, href = html.name, frameCount = frameCount)
    }
}

internal data class StoryboardIndexView(val title: String, val tests: List<IndexEntryView>) : ViewModel {
    override fun template() = super.template() + ".ftl.html"
}

internal data class IndexEntryView(val name: String, val href: String, val frameCount: Int)
