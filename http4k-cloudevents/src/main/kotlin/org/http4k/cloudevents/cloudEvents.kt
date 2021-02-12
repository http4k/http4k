package org.http4k.cloudevents

import io.cloudevents.CloudEvent

fun <T : CloudEvent> T.with(vararg modifiers: (T) -> T): T = modifiers.fold(this) { memo, next -> next(memo) }

