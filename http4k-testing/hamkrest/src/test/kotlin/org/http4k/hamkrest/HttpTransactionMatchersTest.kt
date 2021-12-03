package org.http4k.hamkrest

import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.time.Duration

class HttpTransactionMatchersTest {

    private val tx = HttpTransaction(Request(GET, ""), Response(OK), Duration.ofMillis(1))

    @Test
    fun request() {
        assertMatchAndNonMatch(tx, hasRequest(hasMethod(GET)), hasRequest(hasMethod(POST)))
    }

    @Test
    fun response() = assertMatchAndNonMatch(tx, hasResponse(hasStatus(OK)), hasResponse(hasStatus(INTERNAL_SERVER_ERROR)))
}
