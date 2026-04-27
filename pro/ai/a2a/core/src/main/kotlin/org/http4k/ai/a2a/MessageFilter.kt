/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a

fun interface MessageFilter : (MessageHandler) -> MessageHandler {
    companion object
}

val MessageFilter.Companion.NoOp get() = MessageFilter { it }

fun MessageFilter.then(next: MessageFilter): MessageFilter = MessageFilter { this(next(it)) }

fun MessageFilter.then(next: MessageHandler): MessageHandler = this(next)
