/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

data class SequenceDiagram(
    val participants: List<Participant>,
    val messages: List<SequenceMessage>
)

data class Participant(
    val serviceName: String,
    val index: Int
)

data class SequenceMessage(
    val spanId: String,
    val fromIndex: Int,
    val toIndex: Int,
    val label: String,
    val isResponse: Boolean,
    val isError: Boolean
)
