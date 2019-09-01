package org.http4k.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.or
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Query
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class OrSecurityTest {

    private val callCount = AtomicInteger(0)

    private val next = HttpHandler {
        callCount.incrementAndGet()
        Response(OK)
    }

    private val composite = ApiKeySecurity(Query.required("first"), { true })
        .or(ApiKeySecurity(Query.required("second"), { true }))
        .or(ApiKeySecurity(Query.required("third"), { true }))

    @Test
    fun `toString makes sense`() {
        assertThat(FooBar.or(BarFoo).or(FooBar).toString(), equalTo("OrSecurity(all=[FooBar, BarFoo, FooBar])"))
    }

    @Test
    fun `requires either securities to pass to succeed`() {
        val handler = composite.filter(next)
        assertThat(handler(Request(GET, "")), hasStatus(UNAUTHORIZED))
        assertThat(handler(Request(GET, "?first=true")), hasStatus(OK))
        assertThat(callCount.get(), equalTo(1))
        assertThat(handler(Request(GET, "?second=true")), hasStatus(OK))
        assertThat(callCount.get(), equalTo(2))
        assertThat(handler(Request(GET, "?first=true&second=true")), hasStatus(OK))
        assertThat(callCount.get(), equalTo(3))
    }
}