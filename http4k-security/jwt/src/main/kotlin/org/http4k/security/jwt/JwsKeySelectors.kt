package org.http4k.security.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.source.JWKSourceBuilder
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jose.util.Resource
import com.nimbusds.jose.util.ResourceRetriever
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.lens.Header
import java.io.IOException
import java.net.URI

fun <Context: SecurityContext> http4kJwsKeySelector(
    jwkUri: Uri,
    algorithm: JWSAlgorithm,
    http: HttpHandler = JavaHttpClient()
) = JWSVerificationKeySelector(
    algorithm,
    JWKSourceBuilder.create<Context>(
        URI(jwkUri.toString()).toURL(),
        http4kResourceRetriever(http)
    ).build()
)

fun http4kResourceRetriever(http: HttpHandler) = ResourceRetriever { url ->
    val response = org.http4k.core.Request(Method.GET, url.toString()).let(http)
    if (!response.status.successful) throw IOException("Error retrieving JWK from $url: $response")
    Resource(response.bodyString(), Header.CONTENT_TYPE(response)?.value)
}
