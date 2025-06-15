package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class ComparisonOperator {
    GreaterThanOrEqualToThreshold,
    GreaterThanThreshold,
    LessThanThreshold,
    LessThanOrEqualToThreshold,
    LessThanLowerOrGreaterThanUpperThreshold,
    LessThanLowerThreshold,
    GreaterThanUpperThreshold,
}
