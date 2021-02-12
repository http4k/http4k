package org.http4k.cloudevents

import io.cloudevents.CloudEvent
import org.http4k.core.Response

typealias CEHandler = (CloudEvent) -> Response

fun <T : CloudEvent> T.with(vararg modifiers: (T) -> T): T = modifiers.fold(this) { memo, next -> next(memo) }

