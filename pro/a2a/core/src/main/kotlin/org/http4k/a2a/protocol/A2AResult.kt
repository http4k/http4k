package org.http4k.a2a.protocol

import dev.forkhandles.result4k.Result4k
import org.http4k.a2a.protocol.model.Message
import org.http4k.a2a.protocol.model.Task
import org.http4k.a2a.protocol.model.TaskArtifactUpdateEvent
import org.http4k.a2a.protocol.model.TaskStatusUpdateEvent
import se.ansman.kotshi.JsonSerializable

typealias A2AResult<T> = Result4k<T, A2AError>
