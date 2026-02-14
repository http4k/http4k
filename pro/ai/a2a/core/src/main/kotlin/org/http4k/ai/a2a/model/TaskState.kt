package org.http4k.ai.a2a.model

enum class TaskState {
    submitted,
    working,
    completed,
    failed,
    canceled,
    `input-required`,
    `auth-required`,
    rejected,
    unknown
}
