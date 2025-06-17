package org.http4k.connect.amazon.cloudwatch.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class MetricDataQuery(
    val Id: String,
    val AccountId: String? = null,
    val Expression: String? = null,
    val Label: String? = null,
    val MetricStat: MetricStat? = null,
    val Period: Int? = null,
    val ReturnData: Boolean? = null,
)
