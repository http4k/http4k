package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class ScanBy {
    TimestampDescending,
    TimestampAscending,
}
