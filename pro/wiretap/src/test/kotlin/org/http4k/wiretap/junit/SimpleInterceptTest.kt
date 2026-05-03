/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SimpleInterceptTest {

    private val app = routes("/" bind GET to { Response(OK).body("hello") })

    @RegisterExtension
    @JvmField
    val intercept = Intercept.http(Always) { app }

    @Test
    fun `requests through httpHandler reach the original app`(http: HttpHandler) {
        assertThat(http(Request(GET, "/")).bodyString(), equalTo("hello"))
    }
}
