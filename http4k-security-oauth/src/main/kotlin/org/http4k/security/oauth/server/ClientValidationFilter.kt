package org.http4k.security.oauth.server

import com.natpryce.Failure
import com.natpryce.Success
import com.natpryce.get
import com.natpryce.map
import com.natpryce.mapFailure
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.security.ResponseType

class ClientValidationFilter(private val authoriseRequestValidator: AuthoriseRequestValidator,
                             private val errorRenderer: AuthoriseRequestErrorRender,
                             private val extractor: AuthRequestExtractor) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler =
        {
            if (!validResponseTypes.contains(it.query("response_type"))) {
                errorRenderer.errorFor(it, UnsupportedResponseType(it.query("response_type").orEmpty()))
            } else {
                extractor.extract(it).map { authorizationRequest ->
                    when (val result = authoriseRequestValidator.validate(it, authorizationRequest)) {
                        is Success -> next(result.value)
                        is Failure -> errorRenderer.errorFor(it, result.reason)
                    }
                }.mapFailure { error -> errorRenderer.errorFor(it, error) }.get()
            }
        }

    companion object {
        val validResponseTypes = ResponseType.values().map { it.queryParameterValue }
    }
}

