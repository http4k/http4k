package org.http4k.chaos

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.anything
import org.http4k.core.HttpTransaction
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.http4k.hamkrest.hasQuery
import org.http4k.hamkrest.hasRequest
import org.http4k.hamkrest.hasResponse
import org.http4k.hamkrest.hasStatus
import org.http4k.hamkrest.hasUri
import org.http4k.hamkrest.hasUriPath
import java.time.Duration

/**
{
"type": "request",
"path": "/bob",
"headers": {
"name": "regex"
}
"queries": {
"name": "value"
}
 */

abstract class ChaosTriggerMatcher(val type: String)

data class RequestSpec(val path: Regex?,
                       val headers: Map<String, Regex>?,
                       val queries: Map<String, String>?,
                       val body: Regex?) : ChaosTriggerMatcher("request"), Matcher<HttpTransaction> by {
    val headerMatchers = headers?.map { hasHeader(it.key, it.value) } ?: emptyList()
    val queriesMatchers = queries?.map { hasQuery(it.key, it.value) } ?: emptyList()
    val pathMatchers = path?.let { listOf(hasUri(hasUriPath(it))) } ?: emptyList()
    val bodyMatchers = body?.let { listOf(hasBody(it)) } ?: emptyList()
    hasRequest(
            (headerMatchers + queriesMatchers + pathMatchers + bodyMatchers)
                    .fold<Matcher<Request>, Matcher<Request>>(anything) { acc, next -> acc.and(next) }
    )
}()

data class ResponseSpec(val status: Int?,
                        val headers: Map<String, Regex>?,
                        val body: Regex?) : ChaosTriggerMatcher("response"), Matcher<HttpTransaction> by {
    val headerMatchers = headers?.map { hasHeader(it.key, it.value) } ?: emptyList()
    val statusMatcher = status?.let { listOf(hasStatus((Status(it, "")))) } ?: emptyList()
    val bodyMatchers = body?.let { listOf(hasBody(it)) } ?: emptyList()

    hasResponse(
            (headerMatchers + statusMatcher + bodyMatchers)
                    .fold<Matcher<Response>, Matcher<Response>>(anything) { acc, next -> acc.and(next) }
    )
}()

data class DeadlineSpec(val value: Duration) : ChaosTriggerMatcher("deadline")
data class DelaySpec(val value: Duration) : ChaosTriggerMatcher("delay")

//
//data class ResponseSpec(val path: String, val headers: Map<String, String>, val status: Int) : ChaosTriggerMatcher("response")
//
//data class DeadlineSpec(val value: Duration): ChaosTriggerMatcher("deadline")
//
//data class DelaySpec(val value: Duration): ChaosTriggerMatcher("delay")