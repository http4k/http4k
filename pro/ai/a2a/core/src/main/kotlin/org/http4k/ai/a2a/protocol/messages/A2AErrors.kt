/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import org.http4k.jsonrpc.ErrorMessage

object A2AErrors {
    val TaskNotFound = ErrorMessage(-32001, "Task not found")
    val TaskNotCancelable = ErrorMessage(-32002, "Task not cancelable")
    val PushNotificationNotSupported = ErrorMessage(-32003, "Push notifications not supported")
    val UnsupportedOperation = ErrorMessage(-32004, "Unsupported operation")
    val ContentTypeNotSupported = ErrorMessage(-32005, "Content type not supported")
    val InvalidAgentResponse = ErrorMessage(-32006, "Invalid agent response")
    val ExtendedAgentCardNotConfigured = ErrorMessage(-32007, "Extended agent card not configured")
    val ExtensionSupportRequired = ErrorMessage(-32008, "Extension support required")
    val VersionNotSupported = ErrorMessage(-32009, "Version not supported")
}
