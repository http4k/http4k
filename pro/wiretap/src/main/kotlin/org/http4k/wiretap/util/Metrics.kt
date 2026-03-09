/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.util

import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics
import io.micrometer.core.instrument.binder.system.ProcessorMetrics
import io.micrometer.core.instrument.binder.system.UptimeMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry

fun Metrics() = SimpleMeterRegistry().apply {
    UptimeMetrics().bindTo(this)
    JvmMemoryMetrics().bindTo(this)
    JvmThreadMetrics().bindTo(this)
    JvmGcMetrics().bindTo(this)
    ProcessorMetrics().bindTo(this)
    ClassLoaderMetrics().bindTo(this)
}
