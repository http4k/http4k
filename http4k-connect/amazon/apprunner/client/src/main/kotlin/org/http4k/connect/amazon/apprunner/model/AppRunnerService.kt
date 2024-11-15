package org.http4k.connect.amazon.apprunner.model

import org.http4k.connect.amazon.apprunner.action.Service
import se.ansman.kotshi.JsonSerializable
import java.util.UUID

@JsonSerializable
data class AppRunnerService(
    val OperationId: UUID,
    val Service: Service
)
