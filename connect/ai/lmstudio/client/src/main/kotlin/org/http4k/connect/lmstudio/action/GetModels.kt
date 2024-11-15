@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.lmstudio.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.kClass
import org.http4k.connect.lmstudio.LmStudioAction
import org.http4k.connect.lmstudio.LmStudioMoshi
import org.http4k.connect.lmstudio.ObjectId
import org.http4k.connect.lmstudio.ObjectType
import org.http4k.connect.lmstudio.Org
import org.http4k.core.Method.GET
import org.http4k.core.Request
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
object GetModels : NonNullAutoMarshalledAction<Models>(kClass(), LmStudioMoshi), LmStudioAction<Models> {
    override fun toRequest() = Request(GET, "/v1/models")
}

@JsonSerializable
data class Model(
    val id: ObjectId,
    @JsonProperty(name = "object") val objectType: ObjectType,
    val owned_by: Org,
    val permission: List<Map<String, Any>>? = emptyList(),
)

@JsonSerializable
data class Models(val `data`: List<Model>) {
    @JsonProperty(name = "object")
    val objectType: ObjectType = ObjectType.List
}
