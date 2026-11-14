/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.events

import org.http4k.events.Event

/**
 * Something that went wrong in Fanbelt, carrying its cause. Failures and the recoveries that follow arrive on the
 * runtime's events, for an app's own health check.
 */
interface FanbeltFailure : Event {
    val cause: Throwable
}

data class RequestFailed(override val cause: Throwable) : FanbeltFailure

data class StreamFailed(override val cause: Throwable) : FanbeltFailure
