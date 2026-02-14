package org.http4k.ai.a2a.server.protocol

sealed interface MessageResponse {
    data class Task(val tasks: Sequence<org.http4k.ai.a2a.model.Task>) : MessageResponse {
        constructor(task: org.http4k.ai.a2a.model.Task) : this(sequenceOf(task))
    }

    data class Message(val message: org.http4k.ai.a2a.model.Message) : MessageResponse
}
