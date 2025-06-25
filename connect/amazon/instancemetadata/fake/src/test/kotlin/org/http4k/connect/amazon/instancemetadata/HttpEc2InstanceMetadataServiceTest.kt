package org.http4k.connect.amazon.instancemetadata

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Success
import org.http4k.connect.amazon.instancemetadata.model.Token
import org.http4k.connect.successValue
import org.http4k.core.Method.GET
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test

private class FakeAction: Ec2MetadataAction<String> {
    override fun toRequest() = Request(GET, "/test")

    override fun toResult(response: Response) = Success(response.bodyString())
}

class HttpEc2InstanceMetadataServiceTest {

    private var expectedToken = "token1"

    private val fake = routes(
        "/latest/api/token" bind PUT to {
            Response(OK).body(expectedToken)
        },
        "/test" bind GET to { request ->
            val token = request.header("X-aws-ec2-metadata-token")
            Response(OK).body(token ?: "unauthorized")
        }
    )

    private val client = InstanceMetadataService.Http(
        http = fake,
        tokenProvider = { Success(Token.parse(expectedToken)) }
    )

    @Test
    fun `invoke - uses token from provider`() {
        assertThat(
            client(FakeAction()).successValue(),
            equalTo("token1")
        )
    }

    @Test
    fun `invoke - reacts to new tokens`() {
        assertThat(
            client(FakeAction()).successValue(),
            equalTo("token1")
        )

        expectedToken = "token2"

        assertThat(
            client(FakeAction()).successValue(),
            equalTo("token2")
        )
    }
}
