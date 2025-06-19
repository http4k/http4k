package org.http4k.connect.azure

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.model.ModelName
import org.http4k.ai.model.Role.Companion.System
import org.http4k.ai.model.StopReason
import org.http4k.connect.azure.action.ChatCompletion
import org.http4k.connect.azure.action.Choice
import org.http4k.connect.azure.action.ChoiceDetail
import org.http4k.connect.azure.action.Message
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
class ChatCompletionGeneratorTest {

    private val input = ChatCompletion(
        ModelName.of("Meta-Llama-3.1-70B-Instruct"), listOf(Message.User("foobar"))
    )

    @Test
    fun `lorem ipsum`(approver: Approver) {
        approver.assertApproved(
            ChatCompletionGenerator.LoremIpsum()(input).toString()
        )
    }

    @Test
    fun `reverse input`() {
        assertThat(
            ChatCompletionGenerator.ReverseInput(input),
            equalTo(
                listOf(Choice(0, ChoiceDetail(System, "raboof"), null, StopReason.stop))
            )
        )
    }

    @Test
    fun `echo input`() {
        assertThat(
            ChatCompletionGenerator.Echo(input),
            equalTo(
                listOf(Choice(0, ChoiceDetail(System, "foobar "), null, StopReason.stop))
            )
        )
    }
}
