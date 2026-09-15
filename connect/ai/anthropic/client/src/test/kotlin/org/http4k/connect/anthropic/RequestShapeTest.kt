package org.http4k.connect.anthropic

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import org.http4k.ai.model.MaxTokens
import org.http4k.ai.model.ToolName
import org.http4k.ai.model.UserPrompt
import org.http4k.connect.anthropic.action.CacheControl
import org.http4k.connect.anthropic.action.CacheTtl
import org.http4k.connect.anthropic.action.Caller
import org.http4k.connect.anthropic.action.Citations
import org.http4k.connect.anthropic.action.Content
import org.http4k.connect.anthropic.action.DocumentSource
import org.http4k.connect.anthropic.action.Effort
import org.http4k.connect.anthropic.action.Message
import org.http4k.connect.anthropic.action.MessageCompletion
import org.http4k.connect.anthropic.action.OutputConfig
import org.http4k.connect.anthropic.action.OutputFormat
import org.http4k.connect.anthropic.action.Source
import org.http4k.connect.anthropic.action.ToolCaller
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Uri
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class RequestShapeTest {

    private val minimal =
        MessageCompletion(AnthropicModels.Claude_Opus_5, UserPrompt.of("hi"), MaxTokens.of(10))

    @Test
    fun `a minimal request does not send sampling parameters`() {
        val json = AnthropicAIMoshi.asFormatString(minimal)

        assertThat(json, !containsSubstring("temperature"))
        assertThat(json, !containsSubstring("top_k"))
        assertThat(json, !containsSubstring("top_p"))
    }

    @Test
    fun `a fully loaded request`(approver: Approver) {
        approver.assertApproved(
            AnthropicAIMoshi.asFormatString(
                minimal.copy(
                    system = listOf(Content.Text("you are a helpful bot", CacheControl(CacheTtl.`1h`))),
                    cache_control = CacheControl(),
                    output_config = OutputConfig(Effort.high, OutputFormat(mapOf("type" to "object"))),
                    messages = listOf(
                        Message.Assistant(
                            Content.ToolUse(
                                ToolName.of("tool"),
                                ToolUseId.of("toolu_1"),
                                input = mapOf("foo" to 123),
                                caller = ToolCaller(Caller.direct)
                            ),
                        ),
                        Message.User(
                            listOf(
                                Content.Document(
                                    DocumentSource.Url(Uri.of("https://example.com/report.pdf")),
                                    title = "Q4 report",
                                    citations = Citations(true)
                                ),
                                Content.Image(Source.Url(Uri.of("https://example.com/dog.png"))),
                                Content.ToolResult(
                                    ToolUseId.of("toolu_1"),
                                    listOf(Content.Text("no such city")),
                                    is_error = true
                                )
                            )
                        )
                    )
                )
            ),
            APPLICATION_JSON
        )
    }
}
