package org.http4k.connect.amazon.kms.model

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.amazon.core.model.KMSKeyId
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class KeyEntry(val KeyId: KMSKeyId, val KeyArn: ARN)
