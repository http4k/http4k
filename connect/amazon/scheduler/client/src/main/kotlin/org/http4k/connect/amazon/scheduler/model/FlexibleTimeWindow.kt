package org.http4k.connect.amazon.scheduler.model

import com.squareup.moshi.Json
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class FlexibleTimeWindow(
    @Json(name = "Mode") val mode: TimeWindowMode,
    @Json(name = "MaximumWindowInMinutes") val maximumWindowInMinutes: Int? = null
)


enum class TimeWindowMode { OFF, FLEXIBLE }
