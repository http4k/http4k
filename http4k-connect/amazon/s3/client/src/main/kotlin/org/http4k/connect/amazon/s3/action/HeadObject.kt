package org.http4k.connect.amazon.s3.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.RfcTimestamp
import org.http4k.connect.amazon.s3.S3BucketAction
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.amazon.s3.model.ObjectDetails
import org.http4k.connect.amazon.s3.model.RestoreStatus
import org.http4k.connect.amazon.s3.model.StorageClass
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.HEAD
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.http4k.lens.Header

private val restoreStatusMatcher by lazy {
    "ongoing-request=\"(true|false)\"(?:, expiry-date=\"(.*)\")?".toRegex()
}

@Http4kConnectAction
data class HeadObject(val key: BucketKey) : S3BucketAction<ObjectDetails?> {
    override fun toRequest() = Request(HEAD, uri())

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(ObjectDetails(
                eTag = response.header("etag")?.trim('"'),
                lastModified = response.header("last-modified")?.let(RfcTimestamp::parse),
                contentLength = response.header("content-length")?.toInt(),
                contentType = Header.CONTENT_TYPE(response),
                storageClass = response.header("x-amz-storage-class")?.let { StorageClass.valueOf(it) },
                id = response.header("x-amz-id-2"),
                versionId = response.header("x-amz-version-id"),
                restoreStatus = response.header("x-amz-restore")
                    ?.let { restoreStatusMatcher.find(it) }
                    ?.let {
                        RestoreStatus(
                            ongoingRequest = it.groupValues[1].toBoolean(),
                            expiryDate = it.groupValues.getOrNull(2)
                                ?.takeIf(String::isNotBlank)
                                ?.let(RfcTimestamp::parse)
                        )
                    }
            ))
            status == NOT_FOUND -> Success(null)
            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun uri() = Uri.of("/${key}")
}
