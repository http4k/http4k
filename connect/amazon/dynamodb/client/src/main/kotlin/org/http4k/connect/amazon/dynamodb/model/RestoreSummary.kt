package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class RestoreSummary(
    val RestoreDateTime: Timestamp? = null,
    val RestoreInProgress: Boolean? = null,
    val SourceBackupArn: ARN? = null,
    val SourceTableArn: ARN? = null
)
