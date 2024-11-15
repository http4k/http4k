package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ArchivalSummary(
    val ArchivalBackupArn: ARN? = null,
    val ArchivalDateTime: Timestamp? = null,
    val ArchivalReason: String? = null
)
