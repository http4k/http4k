package org.http4k.filter

import io.kotest.matchers.Matcher
import io.kotest.matchers.should
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.HttpMessage

/**
 * Perform an assertion on the incoming Request as a Filter operation
 */
fun RequestFilters.Assert(match: Matcher<HttpMessage>) = Filter { next ->
    HttpHandler {
        next(it.also { it should match })
    }
}

/**
 * Perform an assertion on the outgoing Response as a Filter operation
 */
fun ResponseFilters.Assert(match: Matcher<HttpMessage>) = Filter { next ->
    HttpHandler {
        next(it).also { it should match }
    }
}
