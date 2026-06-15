package org.http4k.storyboard

import org.http4k.storyboard.otel.SpanSnapshot

data class EventContext(val span: SpanSnapshot, val event: SpanSnapshot.Event)
