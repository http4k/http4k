/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.junit

import App
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.wiretap.junit.RenderMode.Always
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import testRequest

class MultiAppInterceptTest {

    private val downstream: HttpHandler = { Response(OK).body("downstream") }

    @RegisterExtension
    @JvmField
    val intercept = Intercept(downstream, Always) {
        App(App(http(), "test app 2", otel("test app 2")), "test app 1", otel("test app 1"))
    }

    @Test
    fun `requests through factory-built app reach the app`(http: HttpHandler) {
        val response = http(testRequest())
        assertThat(response.bodyString(), equalTo("downstream"))
    }
}
