package org.reekwest.http.basicauth

import org.reekwest.http.base64Decoded
import org.reekwest.http.base64Encode
import org.reekwest.http.core.Filter
import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status

data class Credentials(val user: String, val password: String)

class BasicAuthClient(private val provider: () -> Credentials) : Filter {
    constructor(user: String, password: String) : this(Credentials(user, password))
    constructor(credentials: Credentials) : this({ credentials })

    override fun invoke(handler: HttpHandler): HttpHandler {
        return { request: Request ->
            handler(request.header("Authorization", "Basic ${provider().base64Encoded()}"))
        }
    }

    private fun Credentials.base64Encoded(): String = "$user:$password".base64Encode()
}

class BasicAuthServer(private val realm: String, private val authorize: (Credentials) -> Boolean) : Filter {
    constructor(realm: String, user: String, password: String) : this(realm, Credentials(user, password))
    constructor(realm: String, credentials: Credentials) : this(realm, { it == credentials })

    override fun invoke(handler: HttpHandler): HttpHandler = {
        request: Request ->
        val credentials = request.basicAuthenticationCredentials()
        if (credentials == null || !authorize(credentials)) {
            Response(Status.UNAUTHORIZED).header("WWW-Authenticate", "Basic Realm=\"$realm\"")
        } else {
            handler(request)
        }
    }

    private fun Request.basicAuthenticationCredentials(): Credentials? = header("Authorization")?.replace("Basic ", "")?.toCredentials()

    private fun String.toCredentials(): Credentials? = base64Decoded().split(":").let { Credentials(it.getOrElse(0, { "" }), it.getOrElse(1, { "" })) }
}
