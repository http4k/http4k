package org.http4k.security.oauth.server.accesstoken

import com.natpryce.Result
import com.natpryce.map
import org.http4k.core.Body
import org.http4k.core.Request
import org.http4k.lens.FormField
import org.http4k.lens.Validator
import org.http4k.lens.webForm
import org.http4k.security.AccessTokenDetails
import org.http4k.security.oauth.server.AccessTokenError
import org.http4k.security.oauth.server.AccessTokens
import org.http4k.security.oauth.server.ClientId

class ClientCredentialsAccessTokenGenerator(private val accessTokens: AccessTokens) : AccessTokenGenerator {
    override fun generate(request: Request) = generate(ClientCredentialsForm.extract(request))

    override val rfcGrantType = "client_credentials"

    fun generate(request: ClientCredentialsRequest): Result<AccessTokenDetails, AccessTokenError> =
        accessTokens.create(request.clientId).map { AccessTokenDetails(it) }
}

data class ClientCredentialsRequest(val clientId: ClientId)

private object ClientCredentialsForm {
    private val clientId = FormField.map(::ClientId, ClientId::value).required("client_id")

    val accessTokenForm = Body.webForm(Validator.Strict, clientId).toLens()

    fun extract(request: Request): ClientCredentialsRequest =
        with(accessTokenForm(request)) {
            ClientCredentialsRequest(clientId(this))
        }
}
