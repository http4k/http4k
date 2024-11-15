package org.http4k.connect.amazon.s3.model

import org.http4k.connect.model.Timestamp

data class ObjectSummary(
    val ETag: String? = null,
    val Key: BucketKey,
    val LastModified: Timestamp? = null,
    val DisplayName: String? = null,
    val ID: String? = null,
    val Owner: Owner? = null,
    val Size: Int? = null,
    val StorageClass: StorageClass? = null
)
