package org.http4k.connect.amazon.s3.model

import org.http4k.connect.amazon.core.model.RfcTimestamp
import org.http4k.core.ContentType

data class ObjectDetails(
    val id: String? = null,
    val contentLength: Int? = null,
    val contentType: ContentType? = null,
    val eTag: String? = null,
    val lastModified: RfcTimestamp? = null,
    val restoreStatus: RestoreStatus? = null,
    val storageClass: StorageClass? = null,
    val versionId: String? = null,
)

data class RestoreStatus(
    val ongoingRequest: Boolean,
    val expiryDate: RfcTimestamp?
)
