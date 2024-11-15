package org.http4k.connect.amazon.systemsmanager.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.KMSKeyId
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.systemsmanager.SystemsManagerAction
import org.http4k.connect.amazon.systemsmanager.model.ParameterType
import org.http4k.connect.amazon.systemsmanager.model.SSMParameterName
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class PutParameter(
    val Name: SSMParameterName,
    val Value: String,
    val Type: ParameterType,
    val KeyId: KMSKeyId? = null,
    val Overwrite: Boolean? = null,
    val AllowedPattern: String? = null,
    val DataType: String? = null,
    val Description: String? = null,
    val Policies: List<String>? = null,
    val Tags: List<Tag>? = null,
    val Tier: String? = null
) : SystemsManagerAction<PutParameterResult>(PutParameterResult::class)

@JsonSerializable
data class PutParameterResult(
    val Tier: String,
    val Version: Int
)
