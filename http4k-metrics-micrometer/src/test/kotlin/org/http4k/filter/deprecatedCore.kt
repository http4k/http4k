package org.http4k.filter

@Deprecated("Moved filters onto ClientFilters and ServerFilters")
object MetricFilters {
    @Deprecated("Moved to ServerFilters extension", ReplaceWith("ServerFilters.MicrometerMetrics"))
    val Server = ServerFilters.MicrometerMetrics

    @Deprecated("Moved to ClientFilters extension", ReplaceWith("ClientFilters.MicrometerMetrics"))
    val Client = ClientFilters.MicrometerMetrics
}
