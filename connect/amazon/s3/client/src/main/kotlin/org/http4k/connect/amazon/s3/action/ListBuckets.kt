package org.http4k.connect.amazon.s3.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.core.text
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.amazon.s3.S3Action
import org.http4k.connect.amazon.s3.model.BucketName
import org.http4k.connect.asRemoteFailure
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri

@Http4kConnectAction
class ListBuckets : S3Action<BucketList> {

    override fun toRequest() = Request(GET, uri())

    private fun uri() = Uri.of("/")

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> {
                val buckets = xmlDoc().getElementsByTagName("Name")
                val items = (0 until buckets.length).map { BucketName.of(buckets.item(it).text()) }
                Success(BucketList(items))
            }

            else -> Failure(asRemoteFailure(this))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int = javaClass.hashCode()
}

data class BucketList(val items: List<BucketName>)
