package org.http4k.connect.azure.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.azure.AzureAIAction
import org.http4k.connect.azure.AzureAIMoshi
import org.http4k.connect.azure.ModelProvider
import org.http4k.connect.azure.ModelType
import org.http4k.connect.kClass
import org.http4k.connect.model.ModelName
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.value
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class GetInfo(val modelName: ModelName? = null) : NonNullAutoMarshalledAction<ModelInfo>(kClass(), AzureAIMoshi), AzureAIAction<ModelInfo> {
    override fun toRequest() = Request(GET, "/info").with(
        Header.value(ModelName).optional("x-ms-model-mesh-model-name") of modelName)
}

@JsonSerializable
data class ModelInfo(
    val model_name: ModelName,
    val model_type: ModelType,
    val model_provider_name: ModelProvider
)
