package org.http4k.connect.amazon.s3.action

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.Paged
import org.http4k.connect.PagedAction
import org.http4k.connect.amazon.core.firstChild
import org.http4k.connect.amazon.core.firstChildText
import org.http4k.connect.amazon.core.text
import org.http4k.connect.amazon.core.xmlDoc
import org.http4k.connect.amazon.s3.S3BucketAction
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.amazon.s3.model.ObjectSummary
import org.http4k.connect.amazon.s3.model.Owner
import org.http4k.connect.amazon.s3.model.StorageClass
import org.http4k.connect.asRemoteFailure
import org.http4k.connect.model.Timestamp
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import java.time.Instant

/**
 * List items in a bucket. Note that the S3 API maxes out at 1000 items.
 */
@Http4kConnectAction
data class ListObjectsV2(
    val continuationToken: String? = null,
    val maxKeys: Int? = null,
    val prefix: String? = null,
    val delimiter: String? = null,
    val encodingType: String? = null,
    val expectedBucketOwner: String? = null,
    val requestPayer: String? = null,
) : S3BucketAction<ObjectList>,
    PagedAction<String, ObjectSummary, ObjectList, ListObjectsV2> {
    override fun toRequest() = Request(GET, uri()).query("list-type", "2")
        .let { rq -> continuationToken?.let { rq.query("continuation-token", it) } ?: rq }
        .let { rq -> maxKeys?.let { rq.query("max-keys", it.toString()) } ?: rq }
        .let { rq -> prefix?.let { rq.query("prefix", it) } ?: rq }
        .let { rq -> delimiter?.let { rq.query("delimiter", it) } ?: rq }
        .let { rq -> encodingType?.let { rq.query("encoding-type", it) } ?: rq }
        .let { rq -> expectedBucketOwner?.let { rq.header("x-amz-expected-bucket-owner", it) } ?: rq }
        .let { rq -> requestPayer?.let { rq.header("x-amz-request-payer", it) } ?: rq }

    override fun toResult(response: Response) = with(response) {
        when {
            status.successful -> {
                val xmlDoc = xmlDoc()
                val contents = xmlDoc.getElementsByTagName("Contents")
                val commonPrefixes = xmlDoc.getElementsByTagName("CommonPrefixes")
                Success(
                    ObjectList(
                        (0 until contents.length)
                            .map { contents.item(it) }
                            .map {
                                ObjectSummary(
                                    it.firstChildText("ETag"),
                                    BucketKey.of(it.firstChildText("Key")!!),
                                    it.firstChildText("LastModified")?.let { Timestamp.of(Instant.parse(it)) },
                                    it.firstChildText("DisplayName"),
                                    it.firstChildText("ID"),
                                    it.firstChild("Owner")?.let {
                                        Owner(it.firstChildText("DisplayName"), it.firstChildText("ID"))
                                    },
                                    it.firstChildText("Size")?.toInt(),
                                    it.firstChildText("StorageClass")?.let { StorageClass.valueOf(it) }
                                )
                            },
                        (0 until commonPrefixes.length)
                            .map { commonPrefixes.item(it) }
                            .mapNotNull { it.firstChildText("Prefix") },
                        xmlDoc.getElementsByTagName("NextContinuationToken").item(0)?.text()
                    )
                )
            }

            else -> Failure(asRemoteFailure(this))
        }
    }

    private fun uri() = Uri.of("/")

    override fun next(token: String) = copy(continuationToken = token)
}

data class ObjectList(
    override val items: List<ObjectSummary>,
    val commonPrefixes: List<String> = emptyList(),
    val continuationToken: String? = null
) : Paged<String, ObjectSummary> {
    override fun token() = continuationToken
}

