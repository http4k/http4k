package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class SSEDescription(
    val InaccessibleEncryptionDateTime: Timestamp? = null,
    val KMSMasterKeyArn: ARN? = null,
    val SSEType: SSEType? = null,
    val Status: String? = null
)
