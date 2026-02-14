package org.http4k.ai.a2a.server.protocol

/**
 * Handles incoming messages and produces responses.
 * This is the main extension point for implementing agent behavior.
 * Returns either a Task (with streaming updates) or a direct Message response.
 */
typealias MessageHandler = (MessageRequest) -> MessageResponse
