@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.openai.action

import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.kClass
import org.http4k.connect.model.Timestamp
import org.http4k.connect.openai.ObjectId
import org.http4k.connect.openai.ObjectType
import org.http4k.connect.openai.OpenAIAction
import org.http4k.connect.openai.OpenAIMoshi
import org.http4k.connect.openai.OpenAIOrg
import org.http4k.core.Method.GET
import org.http4k.core.Request
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonProperty
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
object GetModels : NonNullAutoMarshalledAction<Models>(kClass(), OpenAIMoshi), OpenAIAction<Models> {
    override fun toRequest() = Request(GET, "/v1/models")
}

@JsonSerializable
data class Permission(
    val id: ObjectId,
    @JsonProperty(name = "object") val objectType: ObjectType,
    val created: Timestamp,
    val allow_create_engine: Boolean,
    val allow_sampling: Boolean,
    val allow_logprobs: Boolean,
    val allow_search_indices: Boolean,
    val allow_view: Boolean,
    val allow_fine_tuning: Boolean,
    val organization: OpenAIOrg,
    val group: Any?,
    val is_blocking: Boolean
)

@JsonSerializable
data class Model(
    val id: ObjectId,
    @JsonProperty(name = "object") val objectType: ObjectType,
    val created: Timestamp,
    val owned_by: OpenAIOrg,
    val permission: List<Permission>?,
    val root: ObjectId?,
    val parent: Any?
)

@JsonSerializable
data class Models(val `data`: List<Model>) {
    @JsonProperty(name = "object")
    val objectType: ObjectType = ObjectType.List
}
