package org.http4k.connect.amazon.secretsmanager.events

import org.http4k.connect.amazon.secretsmanager.model.SecretId
import se.ansman.kotshi.JsonSerializable
import java.util.UUID

@JsonSerializable
data class SecretsManagerRotationEvent(
    val SecretId: SecretId,
    val Step: Step,
    val ClientRequestToken: UUID? = null,
    val RotationToken: UUID? = null
)

@JsonSerializable
enum class Step { createSecret, setSecret, testSecret, finishSecret }
