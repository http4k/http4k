package org.http4k.connect.amazon.s3.endpoints

import org.http4k.connect.amazon.s3.BucketKeyContent
import org.http4k.connect.amazon.s3.S3Error
import org.http4k.connect.amazon.s3.TestingHeaders.X_HTTP4K_LAST_MODIFIED
import org.http4k.connect.storage.Storage
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_XML
import org.http4k.core.Headers
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.with
import org.http4k.lens.LastModified
import org.http4k.routing.Predicate
import org.http4k.template.PebbleTemplates
import org.http4k.template.viewModel
import java.time.Clock
import java.time.Instant


fun Request.subdomain(buckets: Storage<Unit>): String =
    (header("x-forwarded-host") ?: header("host") ?: uri.host)
        .split('.')
        .firstOrNull()
        ?: run {
            buckets[GLOBAL_BUCKET] = Unit
            GLOBAL_BUCKET
        }

val s3ErrorLens by lazy {
    Body.viewModel(PebbleTemplates().CachingClasspath(), APPLICATION_XML).toLens()
}

const val GLOBAL_BUCKET = "unknown"

internal fun invalidBucketNameResponse() = Response(NOT_FOUND)
    .with(s3ErrorLens of S3Error("NoSuchBucket", message = "The resource you requested does not exist"))

internal fun invalidBucketKeyResponse() = Response(NOT_FOUND)
    .with(s3ErrorLens of S3Error("NoSuchKey", message = "The resource you requested does not exist"))

internal fun invalidObjectStateResponse() = Response(FORBIDDEN)
    .with(s3ErrorLens of S3Error("InvalidObjectState", message = "Object is in an invalid state"))

internal val excludedObjectHeaders = setOf(
    "authorization",
    "x-forwarded-host",
    "x-amz-content-sha256",
    "x-amz-date",
    "x-amz-tagging",
    "x-amz-tagging-directive"
)

internal fun getHeadersWithoutXHttp4kPrefix(it: BucketKeyContent) =
    it.headers.map { it.first.removePrefix("x-http4k-") to it.second }

// TODO may be overlooked that `queries` router only passes if the query has a value
fun queryPresent(name: String) = Predicate("Query present: $name") { req: Request -> req.queries(name).isNotEmpty() }

// TODO may want to consider adding to http4k-core routing.kt
val otherwise = Predicate("Catch-all") { _: Request -> true }

internal fun lastModified(headers: Headers, clock: Clock) = headers
    .firstOrNull { it.first == X_HTTP4K_LAST_MODIFIED }?.second?.let { LastModified.parse(it) }?.value?.toInstant()
    ?: Instant.now(clock)
