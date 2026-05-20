package demo

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.lens.with
import org.junit.jupiter.api.Test
import java.time.LocalDate

class GetCountdownToolTest {

    val clock = TestClock(2026, 1, 1)

    val countdownTool: ToolCapability = GetCountdown(clock)

    @Test
    fun `measure the countdown to Christmas`() {
        val req = ToolRequest()
            .with(eventName of "Christmas")
            .with(date of LocalDate.of(2026, 12, 25))

        val response: ToolResponse = countdownTool(req)

        assertThat(
            output(response as ToolResponse.Ok),
            equalTo(Countdown(358, "Only 358 days until Christmas!"))
        )
    }
}

