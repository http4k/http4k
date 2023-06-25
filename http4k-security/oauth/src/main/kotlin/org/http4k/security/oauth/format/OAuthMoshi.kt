package org.http4k.security.oauth.format

import com.squareup.moshi.Moshi
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.EventAdapter
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.addTyped
import org.http4k.format.asConfigurable
import org.http4k.format.text
import org.http4k.format.withStandardMappings
import org.http4k.security.Nonce
import org.http4k.security.ResponseMode
import org.http4k.security.ResponseType
import org.http4k.security.State
import org.http4k.security.oauth.server.ClientId

object OAuthMoshi : ConfigurableMoshi(
    Moshi.Builder()
        .addTyped(TokenRequestMoshiAdatper)
        .addTyped(AccessTokenResponseMoshiAdapter)
        .addTyped(ErrorResponseMoshiAdapter)
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
