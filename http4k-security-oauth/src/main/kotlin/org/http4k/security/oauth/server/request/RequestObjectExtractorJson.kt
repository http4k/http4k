package org.http4k.security.oauth.server.request

import com.squareup.moshi.Moshi
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.EventAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.text
import org.http4k.format.withStandardMappings
import org.http4k.security.ResponseMode
import org.http4k.security.ResponseType
import org.http4k.security.State
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.openid.Nonce

internal object RequestObjectExtractorJson : ConfigurableMoshi(
    Moshi.Builder()
        .addLast(EventAdapter)
        .asConfigurable()
        .withStandardMappings()
        .text(::ClientId, ClientId::value)
        .text(::State, State::value)
        .text(::Nonce, Nonce::value)
        .text(ResponseMode.Companion::fromQueryParameterValue, ResponseMode::queryParameterValue)
        .text(ResponseType.Companion::fromQueryParameterValue, ResponseType::queryParameterValue)
        .done()
)
