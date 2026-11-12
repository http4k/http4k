/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.events

import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.events.Events
import org.http4k.filter.PolyFilters
import org.http4k.sse.SseResponse

fun ReportFailures(events: Events) = PolyFilters.CatchAll(
    onErrorHttp = {
        events(RequestFailed(it))
        Response(INTERNAL_SERVER_ERROR)
    },
    onErrorSse = {
        events(StreamFailed(it))
        SseResponse(INTERNAL_SERVER_ERROR) { sse -> sse.close() }
    },
)
