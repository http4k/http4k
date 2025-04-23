package org.http4k.a2a.protocol.model

enum class TaskState {
    submitted,
    working,
    `input-required`,
    completed,
    canceled,
    failed,
    unknown
}
