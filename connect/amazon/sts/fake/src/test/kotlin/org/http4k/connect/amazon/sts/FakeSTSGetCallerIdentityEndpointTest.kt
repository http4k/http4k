package org.http4k.connect.amazon.sts

import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FakeSTSGetCallerIdentityEndpointTest {
    private val http = FakeSTS()

    @Test
    fun `get caller identity via form encoded post`() {
        val response = http(
            Request(POST, "/")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .body("Action=GetCallerIdentity&Version=2011-06-15")
        )

        assertTrue(response.status.successful)
        assertTrue(response.bodyString().contains("<GetCallerIdentityResponse"))
        assertTrue(response.bodyString().contains("<Account>123456789012</Account>"))
        assertTrue(response.bodyString().contains("<UserId>ARO123EXAMPLE123:my-role-session-name</UserId>"))
        assertTrue(response.bodyString().contains("<Arn>arn:aws:sts::123456789012:assumed-role/my-role-name/my-role-session-name</Arn>"))
    }

    @Test
    fun `get caller identity via query string is unsupported`() {
        val response = http(
            Request(GET, "/")
                .query("Action", "GetCallerIdentity")
                .query("Version", "2011-06-15")
        )

        assertEquals(Status.NOT_FOUND, response.status)
    }
}
