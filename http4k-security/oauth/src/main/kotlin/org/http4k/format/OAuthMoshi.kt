package org.http4k.format

import com.squareup.moshi.Moshi
import org.http4k.security.Nonce
import org.http4k.security.ResponseMode
import org.http4k.security.ResponseType
import org.http4k.security.State
import org.http4k.security.oauth.server.ClientId

object OAuthMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .addTyped(TokenRequestMoshi)
        .addTyped(AccessTokenResponseMoshi)
        .addTyped(ErrorResponseMoshi)
        .addLast(MapAdapter)
        .addLast(ListAdapter)
        .addLast(EventAdapter)
        .asConfigurable()
        .withStandardMappings()
        .text(::ClientId, ClientId::value)
        .text(::State, State::value)
        .text(::Nonce, Nonce::value)
        .text(ResponseMode::fromQueryParameterValue, ResponseMode::queryParameterValue)
        .text(ResponseType::fromQueryParameterValue, ResponseType::queryParameterValue)
        .done()
)
