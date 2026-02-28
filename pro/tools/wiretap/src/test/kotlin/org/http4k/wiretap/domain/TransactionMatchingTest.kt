package org.http4k.wiretap.domain

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.wiretap.domain.Direction.Inbound
import org.junit.jupiter.api.Test

class TransactionMatchingTest {

    private fun summary(status: Int = 200) = TransactionSummary(
        id = 1,
        direction = Inbound,
        method = "GET",
        uri = "/test",
        path = "/test",
        host = "",
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
}
