/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.fanbelt.infrastructure.Node
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.uri
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable

@EnabledIfEnvironmentVariable(named = "FANBELT_TEST_BROWSER", matches = ".+")
@TestInstance(PER_CLASS)
class RealBrowserTest {
    private fun Request.isBob() = cookie("viewer")?.value == "bob"

    private val app: HttpHandler = routes(
        "/" bind GET to { Response(OK).body(elementReadsPage) },
        "/hidden" bind POST to { if (it.isBob()) Response(FORBIDDEN) else Response(OK) },
    )
    private val server = Node(PolyHandler(app)).asServer(Jetty(0)).start()
    private val browser = RealBrowser(server.uri()).apply { start() }

    @AfterAll
    fun stop() {
        browser.close()
        server.close()
    }

    @Test
    fun `a locator reads as a value type or an enum, and what the type refuses throws`() {
        val page = browser.open("/")
        val shade = page.locator("#shade")
        val colour = page.locator("#colour")
        val neither = page.locator("#neither")

        assertThat(shade.textAs(Shade), equalTo(Shade.of("deep red")))
        assertThat(colour.textAs<Colour>(), equalTo(Colour.Red))
        assertThat(shade.attributeAs("data-shade", Shade), equalTo(Shade.of("deep blue")))
        assertThat(shade.attributeAs<Colour>("data-colour"), equalTo(Colour.Red))
        assertThat({ neither.textAs(Shade) }, throws<IllegalArgumentException>())
        assertThat({ neither.textAs<Colour>() }, throws<IllegalArgumentException>())
        assertThat({ shade.attributeAs("data-bad", Shade) }, throws<IllegalArgumentException>())
        assertThat({ colour.attributeAs("data-shade", Shade) }, throws<IllegalStateException>())
    }

    @Test
    fun `a refusal is proved as the page's own viewer, not as an anonymous visitor`() {
        assertThat(browser.open("/", Cookie("viewer", "bob")).provesServerRefuses("/hidden"), equalTo(Verdict.Denied))
    }
}
