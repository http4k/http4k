package org.http4k.fanbelt.feature

import org.http4k.events.Event

data class OperationRefused(val operation: String, val actor: Any?, val rule: String, val refusal: Refusal) : Event
