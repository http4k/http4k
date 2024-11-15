package org.http4k.connect.amazon.systemsmanager.model

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Parameter(
    val ARN: ARN? = null,
    val Name: SSMParameterName? = null,
    val Value: String? = null,
    val Type: ParameterType? = null,
    val DataType: String? = null,
    val Version: Long? = null,
    val LastModifiedDate: Timestamp? = null,
    val Selector: String? = null,
    val SourceResult: String? = null
)
