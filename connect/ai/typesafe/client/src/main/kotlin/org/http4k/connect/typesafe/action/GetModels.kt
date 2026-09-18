package org.http4k.connect.typesafe.action

import org.http4k.ai.model.ModelName
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.kClass
import org.http4k.connect.typesafe.TypeSafeAction
import org.http4k.connect.typesafe.TypeSafeMoshi
import org.http4k.core.Method.GET
import org.http4k.core.Request
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
object GetModels : NonNullAutoMarshalledAction<Models>(kClass(), TypeSafeMoshi), TypeSafeAction<Models> {
    override fun toRequest() = Request(GET, "/v1/models")
}

@JsonSerializable
data class Models(val models: List<ModelCard>)

@JsonSerializable
data class ModelCard(val name: ModelName, val description: String, val release_date: String)
