package org.http4k.routing.experimental

import java.time.Instant

data class ResourceSummary(val name: String, val lastModified: Instant? = null)
