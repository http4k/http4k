package org.reekwest.http.basicauth

import org.reekwest.http.core.HttpClient
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status
import org.reekwest.http.core.header
import java.util.*

data class Credentials(val user: String, val password: String)

fun HttpClient.basicAuth(user: String, password: String): HttpClient = basicAuth(Credentials(user, password))

fun HttpClient.basicAuth(credentials: Credentials): HttpClient = { request: Request ->
    this(request.header("Authorization", "Basic ${credentials.base64Encoded()}"))
}

fun HttpHandler.basicAuthProtected(realm: String, user: String, password: String): HttpHandler =
    basicAuthProtected(realm, { candidate -> candidate == org.reekwest.http.basicauth.Credentials(user, password) })

fun HttpHandler.basicAuthProtected(realm: String, authorize: (Credentials) -> Boolean): HttpHandler = {
    request: Request ->
    val credentials = request.basicAuthenticationCredentials()
    if (credentials == null || !authorize(credentials)) {
        Response(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic Realm=\"$realm\"")
    } else {
        this(request)
    }
}

private fun Request.basicAuthenticationCredentials(): Credentials? = header("Authorization")?.replace("Basic ", "")?.toCredentials()

private fun String.toCredentials(): Credentials? = base64Decoded().split(":").let { Credentials(it.getOrElse(0, { "" }), it.getOrElse(1, { "" })) }

private fun String.base64Decoded(): String = String(Base64.getDecoder().decode(this))

private fun Credentials.base64Encoded() = String(Base64.getEncoder().encode("$user:$password".toByteArray()))
