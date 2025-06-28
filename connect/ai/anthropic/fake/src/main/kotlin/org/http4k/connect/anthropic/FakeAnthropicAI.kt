package org.http4k.connect.anthropic

import org.http4k.ai.model.ApiKey
import org.http4k.ai.model.ModelName
import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.chaos.start
import org.http4k.connect.anthropic.endpoints.messageCompletion
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.lens.Header

class FakeAnthropicAI(
    completionGenerators: Map<ModelName, MessageContentGenerator> = emptyMap()
) : ChaoticHttpHandler() {

    override val app =
        ServerFilters.ApiKeyAuth(Header.required("x-api-key"), { true })
            .then(messageCompletion(completionGenerators))

    /**
     * Convenience function to get AnthropicAI client
     */
    fun client() = AnthropicAI.Http(
        ApiKey.of("key"),
        ApiVersion._2023_06_01,
        this
    )
}

fun main() {
    FakeAnthropicAI().start()
}
