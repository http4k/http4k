/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import dev.forkhandles.values.UUIDValue
import dev.forkhandles.values.UUIDValueFactory
import java.util.UUID

internal class NodeId private constructor(value: UUID) : UUIDValue(value) {
    companion object : UUIDValueFactory<NodeId>(::NodeId)
}
