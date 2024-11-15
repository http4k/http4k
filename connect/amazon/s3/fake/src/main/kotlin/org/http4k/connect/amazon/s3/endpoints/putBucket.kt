package org.http4k.connect.amazon.s3.endpoints

import org.http4k.connect.storage.Storage
import org.http4k.core.Method.PUT
import org.http4k.core.Response
import org.http4k.core.Status.Companion.CREATED
import org.http4k.routing.bind
import org.http4k.routing.path

fun bucketPutBucket(buckets: Storage<Unit>) = "/" bind PUT to { putBucket(it.subdomain(buckets), buckets) }

fun globalPutBucket(buckets: Storage<Unit>) =
    "/{bucketName}" bind PUT to { putBucket(it.path("bucketName")!!, buckets) }

fun putBucket(bucket: String, buckets: Storage<Unit>): Response {
    buckets[bucket] ?: run { buckets[bucket] = Unit }
    return Response(CREATED)
}
