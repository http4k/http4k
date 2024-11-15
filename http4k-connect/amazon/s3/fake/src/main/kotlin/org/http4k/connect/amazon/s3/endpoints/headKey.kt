package org.http4k.connect.amazon.s3.endpoints

import org.http4k.connect.amazon.core.model.RfcTimestamp
import org.http4k.connect.amazon.s3.BucketKeyContent
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.HEAD
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path

fun pathBasedBucketHeadKey(buckets: Storage<Unit>, bucketContent: Storage<BucketKeyContent>) =
    "/{bucketName}/{bucketKey:.+}" bind HEAD to { req ->
        bucketHeadKey(buckets, req.path("bucketName")!!, bucketContent, req)
    }

fun bucketHeadKey(buckets: Storage<Unit>, bucketContent: Storage<BucketKeyContent>) =
    "/{bucketKey:.+}" bind HEAD to { req ->
        bucketHeadKey(buckets, req.subdomain(buckets), bucketContent, req)
    }

fun bucketHeadKey(
    buckets: Storage<Unit>,
    bucket: String,
    bucketContent: Storage<BucketKeyContent>,
    req: Request
): Response {
    if (buckets[bucket] == null) return invalidBucketNameResponse()
    val obj = bucketContent["${bucket}-${req.path("bucketKey")!!}"] ?: return invalidBucketKeyResponse()
    return Response(OK).headers(getHeadersWithoutXHttp4kPrefix(obj))
        .header("last-modified", RfcTimestamp.of(obj.modified).toString())
}
