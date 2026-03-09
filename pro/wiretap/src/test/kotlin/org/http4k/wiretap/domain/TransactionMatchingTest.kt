/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.wiretap.domain.Direction.Inbound
import org.http4k.wiretap.domain.Direction.Outbound
import org.junit.jupiter.api.Test

class TransactionMatchingTest {

    private fun summary(
        status: Int = 200,
        direction: Direction = Inbound,
        method: String = "GET",
        uri: String = "/test",
        path: String = "/test",
        host: String = ""
    ) = TransactionSummary(
        id = 1,
        direction = direction,
        method = method,
        uri = uri,
        path = path,
        host = host,
        status = status,
        durationMs = 10,
        timestamp = "12:00:00.000",
        isChaos = false,
        isReplay = false,
        chaosInfo = null
    )

    @Test
    fun `null status filter matches everything`() {
        assertThat(summary(200).matches(TransactionFilter(status = null)), equalTo(true))
        assertThat(summary(404).matches(TransactionFilter(status = null)), equalTo(true))
    }

    @Test
    fun `exact status code match`() {
        assertThat(summary(404).matches(TransactionFilter(status = NOT_FOUND)), equalTo(true))
        assertThat(summary(200).matches(TransactionFilter(status = NOT_FOUND)), equalTo(false))
    }

    @Test
    fun `filters by direction`() {
        assertThat(summary(direction = Inbound).matches(TransactionFilter(direction = Inbound)), equalTo(true))
        assertThat(summary(direction = Outbound).matches(TransactionFilter(direction = Inbound)), equalTo(false))
    }

    @Test
    fun `filters by method`() {
        assertThat(summary(method = "GET").matches(TransactionFilter(method = GET)), equalTo(true))
        assertThat(summary(method = "POST").matches(TransactionFilter(method = GET)), equalTo(false))
    }

    @Test
    fun `filters by path substring`() {
        assertThat(summary(uri = "/foo/bar").matches(TransactionFilter(path = "foo")), equalTo(true))
        assertThat(summary(uri = "/baz").matches(TransactionFilter(path = "foo")), equalTo(false))
    }

    @Test
    fun `filters by host substring`() {
        assertThat(summary(host = "example.com").matches(TransactionFilter(host = "example")), equalTo(true))
        assertThat(summary(host = "other.com").matches(TransactionFilter(host = "example")), equalTo(false))
    }

    @Test
    fun `combines multiple fields`() {
        val filter = TransactionFilter(direction = Inbound, method = GET, path = "api")

        assertThat(
            summary(direction = Inbound, method = "GET", uri = "/api/test").matches(filter),
            equalTo(true)
        )
        assertThat(
            summary(direction = Outbound, method = "GET", uri = "/api/test").matches(filter),
            equalTo(false)
        )
        assertThat(
            summary(direction = Inbound, method = "POST", uri = "/api/test").matches(filter),
            equalTo(false)
        )
    }

    @Test
    fun `empty filter matches everything`() {
        assertThat(summary().matches(TransactionFilter()), equalTo(true))
        assertThat(
            summary(status = 500, direction = Outbound, method = "POST", uri = "/x", host = "h")
                .matches(TransactionFilter()),
            equalTo(true)
        )
    }
}
