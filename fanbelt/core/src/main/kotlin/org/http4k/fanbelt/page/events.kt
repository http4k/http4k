package org.http4k.fanbelt.page

import org.http4k.events.Event
import org.http4k.fanbelt.events.FanbeltFailure
import java.time.Duration

data class RedrawFailed(val region: RegionId, override val cause: Throwable) : FanbeltFailure

data class RedrawWaited(val region: RegionId, val waited: Duration) : Event

data class AccessWaited(val rule: String, val waited: Duration) : Event
