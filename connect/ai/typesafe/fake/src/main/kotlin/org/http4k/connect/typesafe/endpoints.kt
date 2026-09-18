package org.http4k.connect.typesafe

import org.http4k.connect.typesafe.TypeSafeMoshi.autoBody
import org.http4k.connect.typesafe.action.ModelCard
import org.http4k.connect.typesafe.action.Models
import org.http4k.connect.typesafe.action.SystemOne
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.bind

fun systemOne(answerer: QuestionAnswerer) = "/v1/systemone" bind POST to { request ->
    val systemOne = autoBody<SystemOne>().toLens()(request)

    Response(OK).with(
        autoBody<SystemOneResponse>().toLens() of SystemOneResponse(
            systemOne.model,
            systemOne.questions.mapValues { (id, question) -> answerer(systemOne.state, id, question) },
            Usage(systemOne.state.approximateTokens(), systemOne.questions.size * 16)
        )
    )
}

fun getModels(models: List<ModelCard>) = "/v1/models" bind GET to {
    Response(OK).with(autoBody<Models>().toLens() of Models(models))
}
