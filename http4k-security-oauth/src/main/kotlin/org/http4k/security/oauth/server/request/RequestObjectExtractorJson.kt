package org.http4k.security.oauth.server.request

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.core.Uri
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.text
import org.http4k.security.ResponseMode
import org.http4k.security.ResponseType
import org.http4k.security.State
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.openid.Nonce

internal object RequestObjectExtractorJson : ConfigurableJackson(
    KotlinModule()
        .asConfigurable()
        .text(Uri.Companion::of, Uri::toString)
        .text(::ClientId, ClientId::value)
        .text(::State, State::value)
        .text(::Nonce, Nonce::value)
        .text(ResponseMode.Companion::fromQueryParameterValue, ResponseMode::queryParameterValue)
        .text(ResponseType.Companion::fromQueryParameterValue, ResponseType::queryParameterValue)
        .done()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
        .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
        .configure(DeserializationFeature.FAIL_ON_MISSING_EXTERNAL_TYPE_ID_PROPERTY, false)
)
