package io.cloudevents

fun <T : CloudEvent> T.with(vararg modifiers: (T) -> T): T = modifiers.fold(this) { memo, next -> next(memo) }
