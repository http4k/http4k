package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ImportSummary(
    val CloudWatchLogGroupArn: ARN? = null,
    val EndTime: Timestamp? = null,
    val ImportArn: ARN? = null,
    val ImportStatus: ImportStatus? = null,
    val InputFormat: InputFormat? = null,
    val S3BucketSource: S3BucketSource? = null,
    val StartTime: Timestamp? = null,
    val TableArn: ARN? = null
)
