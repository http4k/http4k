/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap

import io.opentelemetry.api.OpenTelemetry
import org.http4k.client.JavaHttpClient
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.then
import java.time.Clock
import java.util.Random

/**
 * Provides a deterministic context for the application through test runs.
 */
class Context(
    private val outboundFilter: Filter,
    private val clock: Clock,
    private val random: Random,
    private val oTelFn: (String) -> OpenTelemetry
) {

    /**
     * @return the clock used by the application
     */
    fun clock() = clock

    /**
     * @return wrap or create the outbound HTTP client used by the application
     */
    fun http(http: HttpHandler = JavaHttpClient()) = outboundFilter.then(http)

    /**
     * @return the random number generator used by the application
     */
    fun random() = random

    /**
     * @return create an OpenTelemetry instance with the supplied service name
     */
    fun otel(serviceName: String = "http4k server") = oTelFn(serviceName)
}
