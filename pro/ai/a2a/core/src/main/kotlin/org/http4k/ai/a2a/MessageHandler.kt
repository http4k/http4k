/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a

import org.http4k.ai.a2a.model.MessageRequest
import org.http4k.ai.a2a.model.MessageResponse

/**
 * Handles incoming messages and produces responses.
 * This is the main extension point for implementing agent behavior.
 * Returns either a Task (with streaming updates) or a direct Message response.
 */
typealias MessageHandler = (MessageRequest) -> MessageResponse
