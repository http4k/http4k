package org.http4k.kotest

import io.kotest.matchers.Matcher
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import org.http4k.core.HttpTransaction
import org.http4k.core.Request
import org.http4k.core.Response

infix fun HttpTransaction.shouldHaveRequest(match: Matcher<Request>) = this should haveRequest(match)
infix fun HttpTransaction.shouldNotHaveRequest(match: Matcher<Request>) = this shouldNot haveRequest(match)
fun haveRequest(match: Matcher<Request>): Matcher<HttpTransaction> = match.compose { it.request }

infix fun HttpTransaction.shouldHaveResponse(match: Matcher<Response>) = this should haveResponse(match)
infix fun HttpTransaction.shouldNotHaveResponse(match: Matcher<Response>) = this shouldNot haveResponse(match)
fun haveResponse(match: Matcher<Response>): Matcher<HttpTransaction> = match.compose { it.response }
