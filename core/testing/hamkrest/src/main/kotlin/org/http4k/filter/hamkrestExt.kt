package org.http4k.filter

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.Response

/**
 * Perform an assertThat on the incoming Request as a Filter operation
 */
fun RequestFilters.Assert(matcher: Matcher<Request>) = Filter { next ->
    {
        next(it.also { assertThat(it, matcher) })
    }
}

/**
 * Perform an assertThat on the outgoing Response as a Filter operation
 */
fun ResponseFilters.Assert(matcher: Matcher<Response>) = Filter { next ->
    {
        next(it).also { assertThat(it, matcher) }
    }
}
