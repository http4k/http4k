@file:OptIn(ExperimentalKotshiApi::class)

package org.http4k.connect.typesafe.action

import org.http4k.ai.model.ModelName
import org.http4k.connect.Http4kConnectAction
import org.http4k.connect.NonNullAutoMarshalledAction
import org.http4k.connect.kClass
import org.http4k.connect.typesafe.JevModels.JevLatest
import org.http4k.connect.typesafe.Question
import org.http4k.connect.typesafe.QuestionId
import org.http4k.connect.typesafe.SystemOneResponse
import org.http4k.connect.typesafe.TypeSafeAction
import org.http4k.connect.typesafe.TypeSafeMoshi
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.format.MoshiNode
import org.http4k.format.wrap
import se.ansman.kotshi.ExperimentalKotshiApi
import se.ansman.kotshi.JsonSerializable

@Http4kConnectAction
@JsonSerializable
data class SystemOne(
    val state: MoshiNode,
    val model: ModelName,
    val questions: Map<QuestionId, Question<*>>
) : NonNullAutoMarshalledAction<SystemOneResponse>(kClass(), TypeSafeMoshi),
    TypeSafeAction<SystemOneResponse> {
    override fun toRequest() = Request(POST, "/v1/systemone")
        .with(TypeSafeMoshi.autoBody<SystemOne>().toLens() of this)

    companion object {
        operator fun invoke(state: Any?, model: ModelName = JevLatest, vararg questions: Question<*>) =
            SystemOne(MoshiNode.wrap(state), model, questions.associateBy { it.id })
    }
}
