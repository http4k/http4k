@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class MetricUnit {
    Seconds,
    Microseconds,
    Milliseconds,
    Bytes,
    Kilobytes,
    Megabytes,
    Gigabytes,
    Terabytes,
    Bits,
    Kilobits,
    Megabits,
    Gigabits,
    Terabits,
    Percent,
    Count,
    @JsonProperty("Bytes/Second")
    Bytes_per_Second,
    @JsonProperty("Kilobytes/Second")
    Kilobytes_per_Second,
    @JsonProperty("Megabytes/Second")
    Megabytes_per_Second,
    @JsonProperty("Gigabytes/Second")
    Gigabytes_per_Second,
    @JsonProperty("Terabytes/Second")
    Terabytes_per_Second,
    @JsonProperty("Bits/Second")
    Bits_per_Second,
    @JsonProperty("Kilobits/Second")
    Kilobits_per_Second,
    @JsonProperty("Megabits/Second")
    Megabits_per_Second,
    @JsonProperty("Gigabits/Second")
    Gigabits_per_Second,
    @JsonProperty("Terabits/Second")
    Terabits_per_Second,
    @JsonProperty("Count/Second")
    Count_per_Second,
    None,
}
