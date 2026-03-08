/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import org.http4k.ai.model.StopReason

val StopReason.Companion.end_turn get() = of("end_turn")
val StopReason.Companion.max_tokens get() = of("max_tokens")
val StopReason.Companion.stop_sequence get() = of("stop_sequence")
val StopReason.Companion.tool_use get() = of("tool_use")
