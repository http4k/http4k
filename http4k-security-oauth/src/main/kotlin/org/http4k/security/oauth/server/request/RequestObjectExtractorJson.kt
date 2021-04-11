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

internal fun <T> Map<String, Any>.value(name: String, fn: Function1<String, T>) =
    this[name]?.toString()?.let(fn)
internal fun Map<*, *>.string(name: String) = this[name]?.toString()
internal fun Map<*, *>.boolean(name: String) = this[name]?.toString()?.toBoolean()
internal fun Map<*, *>.long(name: String) = this[name]?.toString()?.toBigDecimal()?.toLong()
@Suppress("UNCHECKED_CAST")
internal fun Map<*, *>.map(name: String) = this[name] as Map<String, Any>?

@Suppress("UNCHECKED_CAST")
internal fun Map<*, *>.strings(name: String) = this[name] as List<String>?
