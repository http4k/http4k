package org.http4k.connect.amazon.systemsmanager.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.amazon.systemsmanager.SystemsManagerAction
import org.http4k.connect.amazon.systemsmanager.model.Parameter
import org.http4k.connect.amazon.systemsmanager.model.SSMParameterName
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class GetParameters(
    val Names: List<SSMParameterName>,
    val WithDecryption: Boolean? = null,
) : SystemsManagerAction<GetParametersResult>(GetParametersResult::class)

@JsonSerializable
data class GetParametersResult(
    val InvalidParameters: List<String>,
    val Parameters: List<Parameter>
)
