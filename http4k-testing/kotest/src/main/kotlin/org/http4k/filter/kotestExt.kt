package org.http4k.filter

import io.kotest.matchers.Matcher
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot
import org.http4k.core.Filter
import org.http4k.core.HttpMessage

/**
 * Perform an assertion on the incoming Request as a Filter operation
 */
fun RequestFilters.Assert(match: Matcher<HttpMessage>) = Filter { next ->
    {
        next(it.also { it should match })
    }
}

/**
 * Perform an assertion on the incoming Request as a Filter operation that should not be satisfied
 */
fun RequestFilters.AssertNot(match: Matcher<HttpMessage>) = Filter { next ->
    {
        next(it.also { it shouldNot match })
    }
}

/**
 * Perform an assertion on the outgoing Response as a Filter operation
 */
fun ResponseFilters.Assert(match: Matcher<HttpMessage>) = Filter { next ->
    {
        next(it).also { it should match }
    }
}

/**
 * Perform an assertion on the outgoing Response as a Filter operation that should not be satisfied
 */
fun ResponseFilters.AssertNot(match: Matcher<HttpMessage>) = Filter { next ->
    {
        next(it).also { it shouldNot match }
    }
}
