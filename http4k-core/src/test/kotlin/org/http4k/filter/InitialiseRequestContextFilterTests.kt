package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import org.http4k.core.Filter
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import org.http4k.hamkrest.hasBody
import org.http4k.lens.RequestContextKey
import org.junit.jupiter.api.Test

class InitialiseRequestContextFilterTests {

    @Test
    fun `initialises request context for use further down the stack`() {
        val contexts = RequestContexts()
        val handler = InitialiseRequestContext(contexts)
            .then(Filter { next ->
                {
                    contexts[it]["foo"] = "manchu"
                    next(it)
                }
            })
            .then { Response(Status.OK).body(contexts[it].get<String>("foo")!!) }

        assertThat(handler(Request(Method.GET, "/")), hasBody("manchu"))
    }

    @Test
    fun `can use request context keys with nested contexts`() {
        val contexts1 = RequestContexts()
        val contexts2 = RequestContexts("second")

        val key1 = RequestContextKey.required<String>(contexts1)
        val key2 = RequestContextKey.required<String>(contexts2)

        val app =
            InitialiseRequestContext(contexts1)
            .then(InitialiseRequestContext(contexts2))
            .then(Filter { next ->
                {
                    next(
                        it.with(key1 of "foo", key2 of "bar")
                    )
                }
            })
            .then {
                Response(Status.OK).body(key1(it) + key2(it)) }

        assertThat(app(Request(Method.GET, "/")), hasBody("foobar"))
    }
}
