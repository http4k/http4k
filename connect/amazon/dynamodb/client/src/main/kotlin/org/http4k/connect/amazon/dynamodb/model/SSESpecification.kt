package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.amazon.core.model.KMSKeyId
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class SSESpecification(
    val Enabled: Boolean,
    val KMSMasterKeyId: KMSKeyId? = null,
    val SSEType: SSEType? = null
)
