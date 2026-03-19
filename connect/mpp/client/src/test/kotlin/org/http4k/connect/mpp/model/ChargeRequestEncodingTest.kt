package org.http4k.connect.mpp.model

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ChargeRequestEncodingTest {

    @Test
    fun `round-trips through base64url encoding`() {
        val chargeRequest = ChargeRequest(
            amount = PaymentAmount.of("1000"),
            currency = Currency.of("USD"),
            recipient = Recipient.of("merchant-42"),
            description = "Premium API access",
            externalId = "order-123",
            methodDetails = mapOf("network" to "mainnet")
        )

        val encoded = chargeRequest.encodeToRequestParam()
        val decoded = encoded.decodeToChargeRequest()

        assertThat(decoded, equalTo(chargeRequest))
    }

    @Test
    fun `round-trips minimal charge request`() {
        val chargeRequest = ChargeRequest(
            amount = PaymentAmount.of("500"),
            currency = Currency.of("EUR")
        )

        val encoded = chargeRequest.encodeToRequestParam()
        val decoded = encoded.decodeToChargeRequest()

        assertThat(decoded, equalTo(chargeRequest))
    }

    @Test
    fun `encoding produces base64url without padding`() {
        val chargeRequest = ChargeRequest(
            amount = PaymentAmount.of("1000"),
            currency = Currency.of("USD")
        )

        val encoded = chargeRequest.encodeToRequestParam()

        assertThat(encoded.contains("="), equalTo(false))
        assertThat(encoded.contains("+"), equalTo(false))
        assertThat(encoded.contains("/"), equalTo(false))
    }
}
