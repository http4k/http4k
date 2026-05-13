/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import org.http4k.template.ViewModel

data class TraceEntry(val traceId: String, val ganttHtml: String, val diagramsHtml: String)

data class TrafficEntry(val trafficHtml: String)

data class JUnitTestReport(
    val testName: String,
    val css: String,
    val traces: List<TraceEntry>,
    val traffic: List<TrafficEntry>,
    val stdOut: String,
    val stdErr: String,
) : ViewModel {
    val markdownFileName: String get() = testName.replace(' ', '-') + ".md"
}
