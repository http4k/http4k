package org.http4k.mcp.model

import org.http4k.ai.model.StopReason

val StopReason.Companion.end_turn get() = of("end_turn")
val StopReason.Companion.max_tokens get() = of("max_tokens")
val StopReason.Companion.stop_sequence get() = of("stop_sequence")
val StopReason.Companion.tool_use get() = of("tool_use")
