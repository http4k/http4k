package org.http4k.ai.llm.chat

import dev.forkhandles.result4k.map
import org.http4k.ai.llm.model.Message
import java.io.PrintStream

fun DebuggingChat(
    delegate: Chat,
    fn: (Message.Assistant) -> String = default,
    stream: PrintStream = System.out
) = Chat {
    delegate(it).map {
        stream.println(fn(it.message))
        it
    }
}

fun Chat.debug() = DebuggingChat(this)

val default: (Message.Assistant) -> String = {
    it.contents.joinToString("\n") + "\n" + it.toolRequests.map { "Tool: ${it.name}(${it.arguments})" }
}
