package org.http4k.connect.amazon.s3.endpoints

import org.http4k.connect.storage.Storage
import org.http4k.core.Method.HEAD
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.path

fun bucketHeadBucket(buckets: Storage<Unit>) =
    "/" bind HEAD to {
        Response(buckets[it.subdomain(buckets)]?.let { OK } ?: NOT_FOUND)
    }


fun globalHeadBucket(buckets: Storage<Unit>) =
    "/{bucketName}" bind HEAD to {
        Response(buckets[it.path("bucketName")!!]?.let { OK } ?: NOT_FOUND)
    }
