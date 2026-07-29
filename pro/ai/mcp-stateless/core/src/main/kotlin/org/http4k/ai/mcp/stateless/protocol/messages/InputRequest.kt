/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.protocol.messages

import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic

/**
 * A request made by a server for input from the client mid-call (MRTR), discriminated on `method`.
 */
@JsonSerializable
@Polymorphic("method")
sealed class InputRequest
