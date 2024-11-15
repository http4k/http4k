package org.http4k.connect.amazon.s3.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.s3.S3BucketAction
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.amazon.s3.model.StorageClass
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Headers
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri

@Http4kConnectAction
data class CopyObject(
    val sourceBucket: BucketName,
    val source: BucketKey,
    val destination: BucketKey,
    val storageClass: StorageClass? = null,
    val tags: List<Tag>? = null,
    val taggingDirective: TaggingDirective? = null,
    val headers: Headers = emptyList()
) :
    S3BucketAction<Unit> {
    override fun toRequest() = Request(PUT, uri())
        .header("x-amz-copy-source", "$sourceBucket/${source}")
        .headers(headers + headersFor(tags.orEmpty()))
        .let { if (taggingDirective == null) it else it.replaceHeader("x-amz-tagging-directive", taggingDirective.toString()) }
        .let { if (storageClass == null) it else it.replaceHeader("x-amz-storage-class", storageClass.toString()) }

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun uri() = Uri.of("/${destination}")
}

enum class TaggingDirective { COPY, REPLACE }
