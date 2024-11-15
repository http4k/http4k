package org.http4k.connect.amazon.cognito.endpoints

import org.http4k.connect.amazon.cognito.CognitoMoshi.autoBody
import org.http4k.connect.amazon.cognito.CognitoPool
import org.http4k.connect.amazon.cognito.Keys
import org.http4k.connect.amazon.cognito.model.Jwk
import org.http4k.connect.amazon.cognito.model.Jwks
import org.http4k.connect.storage.Storage
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.routing.bind
import org.http4k.routing.path
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.UUID

fun wellKnown(storage: Storage<CognitoPool>) =
    "/{poolId}/.well-known/jwks.json" bind GET to { req: Request ->
        (
            req.path("poolId")
                ?.let(storage::get)
                ?.let {
                    Response(OK).with(
                        autoBody<Jwks>().toLens() of Jwks(
                            listOf(
                                Keys.expired.first.asJwk(),
                                Keys.live.first.asJwk(),
                            )
                        )
                    )
                }
                ?: Response(NOT_FOUND)
            )
    }

private fun RSAPublicKey.asJwk() = Jwk(
    e = Base64.getUrlEncoder().encodeToString(publicExponent.toByteArray()),
    kid = UUID.nameUUIDFromBytes(publicExponent.toByteArray()).toString(),
    n = Base64.getUrlEncoder().encodeToString(modulus.toByteArray()),
)
