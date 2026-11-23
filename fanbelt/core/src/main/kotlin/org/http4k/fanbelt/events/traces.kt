/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.events

import org.http4k.events.Event
import org.http4k.fanbelt.bus.Topic
import org.http4k.fanbelt.feature.Outcome
import org.http4k.fanbelt.page.RegionId

/**
 * A step in handling a change: an operation executed, a topic published, a region redrawn or patched into a browser.
 */
sealed interface FanbeltTrace : Event

data class Executed(val operation: String, val actor: String, val outcome: Outcome<*>) : FanbeltTrace

data class Published(val topic: Topic) : FanbeltTrace

data class Patched(val region: RegionId, val actor: String?, val sent: Boolean) : FanbeltTrace

data class Redrawn(val region: RegionId, val key: String, val changed: Boolean) : FanbeltTrace
