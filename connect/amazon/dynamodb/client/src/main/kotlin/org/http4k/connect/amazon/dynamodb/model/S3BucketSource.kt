package org.http4k.connect.amazon.dynamodb.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class S3BucketSource(
    val S3Bucket: String,
    val S3BucketOwner: String? = null,
    val S3KeyPrefix: String? = null
)
