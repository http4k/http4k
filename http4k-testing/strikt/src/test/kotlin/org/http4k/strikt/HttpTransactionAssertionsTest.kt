package org.http4k.strikt

import org.http4k.core.HttpTransaction
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.strikt.duration
import org.http4k.strikt.request
import org.http4k.strikt.response
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.Duration

class HttpTransactionAssertionsTest {

    @Test
    fun assertions() {
        val tx = HttpTransaction(Request(Method.GET, ""), Response(OK), Duration.ZERO)

        expectThat(tx) {
            request.isEqualTo(tx.request)
            response.isEqualTo(tx.response)
            duration.isEqualTo(tx.duration)
        }
    }
}
