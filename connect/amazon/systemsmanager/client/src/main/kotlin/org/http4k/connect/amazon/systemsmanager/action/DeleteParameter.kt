package org.http4k.connect.amazon.systemsmanager.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.systemsmanager.SystemsManagerAction
import org.http4k.connect.amazon.systemsmanager.model.SSMParameterName
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class DeleteParameter(val Name: SSMParameterName) : SystemsManagerAction<Unit>(Unit::class)
