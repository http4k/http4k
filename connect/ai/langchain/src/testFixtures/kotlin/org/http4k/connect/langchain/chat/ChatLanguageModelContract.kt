package org.http4k.connect.langchain.chat

import dev.langchain4j.chain.ConversationalChain
import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.memory.chat.MessageWindowChatMemory
import dev.langchain4j.model.chat.ChatLanguageModel
import org.http4k.testing.ApprovalTest
import org.http4k.testing.Approver
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ApprovalTest::class)
interface ChatLanguageModelContract {

    val model: ChatLanguageModel

    @Test
    fun `can call through to language model`(approver: Approver) {
        val content1 = model.generate("what is 1+1? just answer with the number")
        val content2 = model.generate("what is 2+2? just answer with the number")
        approver.assertApproved(listOf(content1, content2).joinToString("\n"))
    }

    @Test
    fun `can use the model in a chain`(approver: Approver) {
        val chain = ConversationalChain.builder()
            .chatLanguageModel(model)
            .chatMemory(MessageWindowChatMemory.builder().maxMessages(10).build().apply {
                add(SystemMessage("just answer with a number"))
                add(UserMessage("what is 1+1"))
                add(AiMessage("2"))
            })
            .build()
        val content1 = chain.execute("Double it")
        val content2 = chain.execute("Double it")
        approver.assertApproved(listOf(content1, content2).joinToString("\n"))
    }
}
