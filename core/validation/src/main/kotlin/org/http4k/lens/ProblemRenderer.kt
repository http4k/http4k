package org.http4k.lens

import dev.forkhandles.values.Problem
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with

fun interface ProblemRenderer {
    fun render(problems: List<Problem>, status: Status, request: Request): Response

    companion object {
        val Text = ProblemRenderer { problems, status, _ ->
            Response(status).body(problems.joinToString("; ") { it.message })
        }

        fun Rfc9457(body: BiDiBodyLens<ProblemDetails>): ProblemRenderer =
            ProblemRenderer { problems, status, request ->
                Response(status)
                    .with(
                        body of ProblemDetails(
                            "https://http4k.org/problems/validation-error",
                            "Request validation failed",
                            status.code,
                            request.uri.path,
                            problems.map { InvalidParam(it.constraint, it.message, it.value.takeIf(String::isNotEmpty)) }
                        )
                    )
                    .replaceHeader("Content-Type", "application/problem+json")
            }
    }
}

data class ProblemDetails(
    val type: String,
    val title: String,
    val status: Int,
    val instance: String?,
    val invalidParams: List<InvalidParam> = emptyList()
)

data class InvalidParam(
    val name: String,
    val reason: String,
    val value: String? = null
)
