package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.amazon.core.model.ARN
import org.http4k.connect.model.Timestamp
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ImportTableDescription(
    val ClientToken: ClientToken? = null,
    val CloudWatchLogGroupArn: ARN? = null,
    val EndTime: Timestamp? = null,
    val ErrorCount: Long? = null,
    val FailureCode: String? = null,
    val FailureMessage: String? = null,
    val ImportArn: ARN? = null,
    val ImportedItemCount: Long? = null,
    val ImportStatus: ImportStatus? = null,
    val InputCompressionType: InputCompressionType? = null,
    val InputFormat: InputFormat? = null,
    val InputFormatOptions: InputFormatOptions? = null,
    val ProcessedItemCount: Long? = null,
    val ProcessedSizeBytes: Long? = null,
    val S3BucketSource: S3BucketSource? = null,
    val StartTime: Timestamp? = null,
    val TableArn: ARN? = null,
    val TableCreationParameters: TableCreationParameters? = null,
    val TableId: String? = null,
) {
    companion object
}

