@file:Suppress("unused")

package demo

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.orThrow
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.model.ToolName
import org.http4k.lens.with
import org.http4k.wiretap.junit.Intercept
import org.http4k.wiretap.junit.mcpCapabilities
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.time.LocalDate

class GetCountdownClientTest {

    @RegisterExtension
    val intercept = Intercept.mcpCapabilities { GetCountdown(TestClock(2026, 1, 1)) }

    @Test
    fun `Countdown!`(mcpClient: McpClient) {
        mcpClient.start()

        val req = ToolRequest()
            .with(eventName of "KotlinConf 2026")
            .with(date of LocalDate.of(2026, 5, 21))

        val response = mcpClient.tools().call(ToolName.of("countdown"), req).orThrow { error(it) }

        assertThat(
            output(response as ToolResponse.Ok),
            equalTo(Countdown(141, "Only 141 days until Christmas!"))
        )
    }
}
