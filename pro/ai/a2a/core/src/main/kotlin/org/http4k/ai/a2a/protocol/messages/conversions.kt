/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.model.Artifact
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.model.toWire

fun A2AMessage.toDomain() = Message(messageId, role, parts.map { it.toDomain() }, contextId, taskId, metadata, extensions, referenceTaskIds)
fun A2ATaskStatus.toDomain() = TaskStatus(state, message?.toDomain(), timestamp)
fun A2AArtifact.toDomain() = Artifact(artifactId, parts.map { it.toDomain() }, name, description, metadata, extensions)
fun A2ATask.toDomain() = Task(id, contextId, status.toDomain(), artifacts?.map { it.toDomain() }, history?.map { it.toDomain() }, metadata)

fun Message.toWire() = A2AMessage(messageId, role, parts.map { it.toWire() }, contextId, taskId, metadata, extensions, referenceTaskIds)
fun TaskStatus.toWire() = A2ATaskStatus(state, message?.toWire(), timestamp)
fun Artifact.toWire() = A2AArtifact(artifactId, parts.map { it.toWire() }, name, description, metadata, extensions)
fun Task.toWire() = A2ATask(id, contextId, status.toWire(), artifacts?.map { it.toWire() }, history?.map { it.toWire() }, metadata)
