package org.http4k.lens

interface McpLensTarget

fun <T : McpLensTarget> T.with(vararg modifiers: (T) -> T): T = modifiers.fold(this) { memo, next -> next(memo) }

