package org.http4k.ai.a2a.server.protocol

fun interface MessageFilter : (MessageHandler) -> MessageHandler {
    companion object
}

val MessageFilter.Companion.NoOp get() = MessageFilter { it }

fun MessageFilter.then(next: MessageFilter): MessageFilter = MessageFilter { this(next(it)) }

fun MessageFilter.then(next: MessageHandler): MessageHandler = this(next)
