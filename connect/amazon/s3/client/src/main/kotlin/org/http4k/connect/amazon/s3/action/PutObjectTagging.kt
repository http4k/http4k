package org.http4k.connect.amazon.s3.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.s3.S3BucketAction
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.appendAll
import org.http4k.connect.asRemoteFailure
import org.http4k.core.MemoryBody
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri

@Http4kConnectAction
class PutObjectTagging(val key: BucketKey, val tags: List<Tag>): S3BucketAction<Unit> {
    override fun toRequest(): Request {
        return Request(PUT, Uri.of("/$key?tagging")).body(bodyFor(tags))
    }

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> Success(Unit)
            else -> Failure(asRemoteFailure(this))
        }
    }
}

fun bodyFor(tags: List<Tag>) = StringBuilder("<Tagging xmlns=\"http://s3.amazonaws.com/doc/2006-03-01/\">")
    .append("<TagSet>")
    .appendAll(tags) { (key, value) -> "<Tag><Key>$key</Key><Value>$value</Value></Tag>" }
    .append("</TagSet>")
    .append("</Tagging>")
    .let { MemoryBody(it.toString()) }
