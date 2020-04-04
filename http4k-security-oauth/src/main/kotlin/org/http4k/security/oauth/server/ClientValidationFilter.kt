package org.http4k.security.oauth.server

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.security.ResponseType
import org.http4k.util.Failure
import org.http4k.util.Success
import org.http4k.util.map
import org.http4k.util.recover

class ClientValidationFilter(private val authoriseRequestValidator: AuthoriseRequestValidator,
                             private val errorRenderer: AuthoriseRequestErrorRender,
                             private val extractor: AuthRequestExtractor) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler =
        {
            if (!validResponseTypes.contains(it.query("response_type"))) {
                errorRenderer.errorFor(it, UnsupportedResponseType(it.query("response_type").orEmpty()))
            } else {
                extractor.extract(it).map { authorizationRequest ->
                    when (val result = MustHaveRedirectUri(authoriseRequestValidator).validate(it, authorizationRequest)) {
                        is Success -> next(result.value)
                        is Failure -> errorRenderer.errorFor(it, result.reason)
                    }
                }.recover { error -> errorRenderer.errorFor(it, error) }
            }
        }

    companion object {
        val validResponseTypes = ResponseType.values().map { it.queryParameterValue }
    }
}

