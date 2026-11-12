/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.http4k.events.Event
import org.http4k.fanbelt.events.FanbeltFailure
import java.time.Duration

data class RedrawFailed(val region: RegionId, override val cause: Throwable) : FanbeltFailure

data class RedrawWaited(val region: RegionId, val waited: Duration) : Event
