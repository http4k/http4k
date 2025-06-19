package org.http4k.lens

/**
 * Represents a target for a Lens to extract or inject values from.
 */
interface McpLensTarget

fun <T : McpLensTarget> T.with(vararg modifiers: (T) -> T): T = modifiers.fold(this) { memo, next -> next(memo) }

