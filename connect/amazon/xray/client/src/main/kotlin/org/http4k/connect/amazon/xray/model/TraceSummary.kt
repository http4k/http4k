package org.http4k.connect.amazon.xray.model

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.AwsAccount
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class TraceSummary(
    val Id: TraceId,
    val StartTime: Timestamp? = null,
    val Duration: Double? = null,
    val ResponseTime: Double? = null,
    val HasFault: Boolean? = null,
    val HasError: Boolean? = null,
    val HasThrottle: Boolean? = null,
    val IsPartial: Boolean? = null,
    val MatchedEventTime: Timestamp? = null,
    val Http: TraceHttp? = null,
    val Annotations: Map<String, List<ValueWithServiceIds>>? = null,
    val Users: List<TraceUser>? = null,
    val ResourceARNs: List<ResourceARNDetail>? = null,
    val ServiceIds: List<ServiceId>? = null,
)

@JsonSerializable
data class TraceHttp(
    val HttpURL: String? = null,
    val HttpStatus: Int? = null,
    val HttpMethod: String? = null,
    val UserAgent: String? = null,
    val ClientIp: String? = null,
)

@JsonSerializable
data class ValueWithServiceIds(
    val AnnotationValue: AnnotationValue? = null,
    val ServiceIds: List<ServiceId>? = null,
)

@JsonSerializable
data class AnnotationValue(
    val StringValue: String? = null,
    val NumberValue: Double? = null,
    val BooleanValue: Boolean? = null,
)

@JsonSerializable
data class ServiceId(
    val Name: String? = null,
    val Names: List<String>? = null,
    val AccountId: AwsAccount? = null,
    val Type: String? = null,
)

@JsonSerializable
data class TraceUser(
    val UserName: String? = null,
    val ServiceIds: List<ServiceId>? = null,
)

@JsonSerializable
data class ResourceARNDetail(
    val ARN: ARN? = null,
)
