package org.http4k.kotest

import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class HttpTransactionMatchersTest {

    private val tx = HttpTransaction(
        request = Request(GET, ""),
        response = Response(OK),
        start = Instant.ofEpochSecond(1),
        duration = Duration.ofMillis(1)
    )
    
    @Test
    fun request() {
        assertMatchAndNonMatch(tx, haveRequest(haveMethod(GET)), haveRequest(haveMethod(POST)))
    }

    @Test
    fun response() = assertMatchAndNonMatch(tx, haveResponse(haveStatus(OK)), haveResponse(haveStatus(INTERNAL_SERVER_ERROR)))
}
