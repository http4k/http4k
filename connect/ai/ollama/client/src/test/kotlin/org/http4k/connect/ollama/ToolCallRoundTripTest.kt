package org.http4k.connect.ollama

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class ToolCallRoundTripTest {
    @Test
    fun `assistant tool calls survive a message round trip`() {
        val json = """
            {
              "role":"assistant",
              "content":"",
              "tool_calls":[{
                "id":"call_1",
                "function":{"index":0,"name":"get_weather","arguments":{"city":"Paris"}}
              }]
            }
        """.trimIndent()

        val message = OllamaMoshi.asA<Message>(json)
        assertThat(
            OllamaMoshi.asA<Map<String, Any>>(OllamaMoshi.asFormatString(message)),
            equalTo(OllamaMoshi.asA<Map<String, Any>>(json))
        )
    }
}
