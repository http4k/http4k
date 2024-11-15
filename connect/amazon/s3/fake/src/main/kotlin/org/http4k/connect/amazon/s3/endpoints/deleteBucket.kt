package org.http4k.connect.amazon.s3.endpoints

import org.http4k.connect.storage.Storage
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind

fun bucketDeleteBucket(buckets: Storage<Unit>) =
    "/" bind Method.DELETE to {
        if (buckets.remove(it.subdomain(buckets))) Response(Status.OK)
        else invalidBucketNameResponse()
    }
