package org.http4k.connect.amazon.s3.endpoints

import org.http4k.connect.amazon.core.model.Tag
import org.http4k.connect.amazon.s3.BucketKeyContent
import org.http4k.connect.amazon.s3.action.tagsFor
import org.http4k.connect.amazon.s3.model.BucketKey
import org.http4k.connect.storage.Storage
import org.http4k.core.Headers
import org.http4k.core.Method.PUT
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import java.time.Clock
import java.util.Base64

internal fun bucketPutKey(buckets: Storage<Unit>, bucketContent: Storage<BucketKeyContent>, clock: Clock) =
    "/{bucketKey:.+}" bind PUT to routes(
        queryPresent("tagging") bind {
            putObjectTagging(
                it.subdomain(buckets),
                it.path("bucketKey")!!,
                buckets,
                bucketContent,
                clock,
                it.headers,
                tagsFor(it.body),
            )
        },
        otherwise bind {
            putObject(
                it.subdomain(buckets),
                it.path("bucketKey")!!,
                it.body.payload.array(),
                buckets,
                bucketContent,
                clock,
                it.headers,
                tagsFor(it.headers)
            )
        }
    )

internal fun pathBasedBucketPutKey(buckets: Storage<Unit>, bucketContent: Storage<BucketKeyContent>, clock: Clock) =
    "/{bucketName}/{bucketKey:.+}" bind PUT to routes(
        queryPresent("tagging") bind {
            putObjectTagging(
                it.path("bucketName")!!,
                it.path("bucketKey")!!,
                buckets,
                bucketContent,
                clock,
                it.headers,
                tagsFor(it.body),
            )
        },
        otherwise bind {
            putObject(
                it.path("bucketName")!!,
                it.path("bucketKey")!!,
                it.body.payload.array(),
                buckets,
                bucketContent,
                clock,
                it.headers,
                tagsFor(it.headers)
            )
        }
    )

internal fun putObject(
    bucket: String,
    key: String,
    bytes: ByteArray,
    buckets: Storage<Unit>,
    bucketContent: Storage<BucketKeyContent>,
    clock: Clock,
    headers: Headers,
    tags: List<Tag>
) = buckets[bucket]
    ?.let {
        bucketContent["$bucket-$key"] = BucketKeyContent(
            BucketKey.of(key),
            Base64.getEncoder().encodeToString(bytes),
            lastModified(headers, clock),
            headers.filter { it.first !in excludedObjectHeaders },
            tags
        )
        Response(CREATED)
    }
    ?: invalidBucketNameResponse()

private fun putObjectTagging(
    bucket: String,
    key: String,
    buckets: Storage<Unit>,
    bucketContent: Storage<BucketKeyContent>,
    clock: Clock,
    headers: Headers,
    tags: List<Tag>
): Response {
    if (buckets[bucket] == null) return invalidBucketNameResponse()
    val obj = bucketContent["$bucket-$key"] ?: return invalidBucketKeyResponse()

    bucketContent["$bucket-$key"] = obj.copy(
        modified = lastModified(headers, clock),
        tags = tags
    )

    return Response(OK)
}

internal fun tagsFor(headers: Headers) = headers
    .find { it.first.equals("x-amz-tagging", true) }
    ?.second
    ?.split("&")
    .orEmpty()
    .filter { it.isNotEmpty() }
    .map {
        val (key, value) = it.split("=")
        Tag(key, value)
    }
