package org.http4k.contract

import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.contract.security.ApiKeySecurity
import org.http4k.contract.security.Security
import org.http4k.contract.security.and
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.NoOp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasStatus
import org.http4k.lens.Query
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

object FooBar : Security {
    override val filter = Filter.NoOp
    override fun toString() = "FooBar"
}

object BarFoo : Security {
    override val filter = Filter.NoOp
    override fun toString() = "BarFoo"
}

class AndSecurityTest {
    private val callCount = AtomicInteger(0)

    private val next: HttpHandler = {
        callCount.incrementAndGet()
        Response(OK).body("hello")
    }

    private val composite =
        ApiKeySecurity(Query.required("first"), { true })
            .and(ApiKeySecurity(Query.required("second"), { true }))
            .and(ApiKeySecurity(Query.required("third"), { true }))

    @Test
    fun `toString makes sense`() {
        assertThat(FooBar.and(BarFoo).and(FooBar).toString(), equalTo("AndSecurity(all=[FooBar, BarFoo, FooBar])"))
    }

    @Test
    fun `requires both securities to pass to succeed`() {
        val handler = composite.filter(next)
        assertThat(handler(Request(GET, "")), hasStatus(UNAUTHORIZED))
        assertThat(handler(Request(GET, "?first=true")), hasStatus(UNAUTHORIZED))
        assertThat(handler(Request(GET, "?second=true")), hasStatus(UNAUTHORIZED))
        assertThat(handler(Request(GET, "?third=true")), hasStatus(UNAUTHORIZED))
        assertThat(callCount.get(), equalTo(0))
        assertThat(handler(Request(GET, "?first=true&second=true&third=true")), hasStatus(OK).and(hasBody("hello")))
        assertThat(callCount.get(), equalTo(1))
    }
}
