package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.RemoteFailure
import org.http4k.connect.mpp.MppMoshi
import org.http4k.connect.mpp.MppVerifier
import org.http4k.connect.mpp.model.Challenge
import org.http4k.connect.mpp.model.ChallengeId
import org.http4k.connect.mpp.model.ChargeRequest
import org.http4k.connect.mpp.model.Credential
import org.http4k.connect.mpp.model.Currency
import org.http4k.connect.mpp.model.MppProblem
import org.http4k.connect.mpp.model.PaymentAmount
import org.http4k.connect.mpp.model.PaymentIntent
import org.http4k.connect.mpp.model.PaymentMethod
import org.http4k.connect.mpp.model.Realm
import org.http4k.connect.mpp.model.Receipt
import org.http4k.connect.mpp.model.ReceiptStatus
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.PAYMENT_REQUIRED
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.lens.mppChallengeLens
import org.http4k.lens.mppCredentialLens
import org.http4k.lens.mppReceiptLens
import org.junit.jupiter.api.Test
import java.time.Instant

class MppServerFilterTest {

    private val challenge = Challenge(
        id = ChallengeId.of("challenge-123"),
        realm = Realm.of("api.example.com"),
        method = PaymentMethod.of("tempo"),
        intent = PaymentIntent.of("charge"),
        request = ChargeRequest(amount = PaymentAmount.of("1000"), currency = Currency.of("USD"))
    )

    private val validCredential = Credential(
        challenge = challenge,
        payload = mapOf("proof" to "0xsigned")
    )

    private val receipt = Receipt(
        status = ReceiptStatus.success,
        method = PaymentMethod.of("tempo"),
        timestamp = Instant.parse("2025-01-15T12:05:00Z"),
        challengeId = ChallengeId.of("challenge-123")
    )

    @Test
    fun `no authorization header returns 402 with challenge and problem details`() {
        val handler = ServerFilters.MppPaymentRequired(
            verifier = MppVerifier { Success(receipt) },
            challengeFor = { challenge }
        ).then { Response(OK).body("content") }

        val response = handler(Request(GET, "/"))

        assertThat(response.status, equalTo(PAYMENT_REQUIRED))
        assertThat(response.header("Cache-Control"), equalTo("no-store"))
        val returnedChallenge = mppChallengeLens(response)
        assertThat(returnedChallenge.id, equalTo(ChallengeId.of("challenge-123")))
        assertThat(returnedChallenge.realm, equalTo(Realm.of("api.example.com")))
        assertThat(returnedChallenge.method, equalTo(PaymentMethod.of("tempo")))
        val problem = MppMoshi.asA<MppProblem>(response.bodyString())
        assertThat(problem, equalTo(MppProblem.paymentRequired))
    }

    @Test
    fun `valid credential returns 200 with receipt`() {
        val handler = ServerFilters.MppPaymentRequired(
            verifier = MppVerifier { Success(receipt) },
            challengeFor = { challenge }
        ).then { Response(OK).body("content") }

        val response = handler(Request(GET, "/").with(mppCredentialLens of validCredential))

        assertThat(response.status, equalTo(OK))
        assertThat(response.bodyString(), equalTo("content"))
        val returnedReceipt = mppReceiptLens(response)
        assertThat(returnedReceipt.status, equalTo(ReceiptStatus.success))
        assertThat(returnedReceipt.method, equalTo(PaymentMethod.of("tempo")))
        assertThat(returnedReceipt.challengeId, equalTo(ChallengeId.of("challenge-123")))
    }

    @Test
    fun `verification failure returns 402 with verification-failed problem`() {
        val handler = ServerFilters.MppPaymentRequired(
            verifier = MppVerifier { Failure(RemoteFailure(POST, Uri.of("https://verify.example.com"), BAD_REQUEST, "bad signature")) },
            challengeFor = { challenge }
        ).then { Response(OK).body("content") }

        val response = handler(Request(GET, "/").with(mppCredentialLens of validCredential))

        assertThat(response.status, equalTo(PAYMENT_REQUIRED))
        assertThat(response.header("Cache-Control"), equalTo("no-store"))
        val problem = MppMoshi.asA<MppProblem>(response.bodyString())
        assertThat(problem, equalTo(MppProblem.verificationFailed))
    }
}
