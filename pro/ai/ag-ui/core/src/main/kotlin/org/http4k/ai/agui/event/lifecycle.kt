/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.agui.event

import org.http4k.ai.agui.model.RunAgentInput
import org.http4k.ai.agui.model.RunId
import org.http4k.ai.agui.model.StepName
import org.http4k.ai.agui.model.ThreadId
import org.http4k.format.MoshiNode
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@PolymorphicLabel("RUN_STARTED")
data class RunStarted(
    val threadId: ThreadId,
    val runId: RunId,
    val parentRunId: RunId? = null,
    val input: RunAgentInput? = null,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("RUN_FINISHED")
data class RunFinished(
    val threadId: ThreadId,
    val runId: RunId,
    val result: MoshiNode? = null,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("RUN_ERROR")
data class RunError(
    val message: String,
    val code: String? = null,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("STEP_STARTED")
data class StepStarted(
    val stepName: StepName,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()

@JsonSerializable
@PolymorphicLabel("STEP_FINISHED")
data class StepFinished(
    val stepName: StepName,
    override val timestamp: Long? = null,
    override val rawEvent: Any? = null
) : AgUiEvent()
