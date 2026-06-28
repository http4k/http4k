/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package passkeys

import freemarker.template.Configuration.VERSION_2_3_34
import org.http4k.connect.model.Base64UriBlob
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.TEXT_HTML
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ServerFilters
import org.http4k.lens.FormField
import org.http4k.lens.RequestKey
import org.http4k.lens.Validator
import org.http4k.lens.location
import org.http4k.lens.webForm
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.passkeys.Passkeys
import org.http4k.security.passkeys.Principal
import org.http4k.security.passkeys.Principals
import org.http4k.security.passkeys.model.RelyingParty
import org.http4k.security.passkeys.testing.InMemoryPasskeyPersistence
import org.http4k.security.passkeys.testing.InsecureCookieBasedPrincipals
import org.http4k.security.passkeys.util.PasskeysJson.json
import org.http4k.security.passkeys.webauthn4j.WebAuthn4jPasskeyVerifier
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.template.FreemarkerTemplates
import org.http4k.template.FreemarkerTemplates.Companion.safeConfiguration
import org.http4k.template.ViewModel
import org.http4k.template.viewModel

object LoginView : ViewModel { override fun template() = "login.ftl" }
object SignupView : ViewModel { override fun template() = "signup.ftl" }
data class ProtectedView(val user: String?) : ViewModel { override fun template() = "protected.ftl" }

/** The signup form the browser POSTs to /passkeys/register/options. */
data class SignupRequest(val email: String, val firstName: String, val lastName: String)

private val username = FormField.required("username")
private val password = FormField.required("password")
private val loginForm = Body.webForm(Validator.Strict, username, password).toLens()

/**
 * Runnable demo of 2 modes:
 * - log in with a password then add a passkey
 * - sign up passwordlessly with a passkey
 */
fun main() {
    val rp = RelyingParty(id = "localhost", name = "http4k passkeys demo", origin = Uri.of("http://localhost:9000"))
    val persistence = InMemoryPasskeyPersistence()
    val accounts = Accounts()
    val contextKey = RequestKey.required<Base64UriBlob>("handle")
    val session = InsecureCookieBasedPrincipals("http4k", contextKey)

    val renderer = FreemarkerTemplates(safeConfiguration(VERSION_2_3_34)).CachingClasspath("passkeys")
    val html = Body.viewModel(renderer, TEXT_HTML).toLens()

    val addPasskey = { request: Request ->
        (session.read(request) as? Principal.Known)?.let { accounts.userForHandle(it.userHandle) }
    }
    val passkeys = Passkeys.passwordless(
        rp,
        WebAuthn4jPasskeyVerifier(),
        persistence,
        session,
        user = { req ->
            addPasskey(req) ?: runCatching { req.json<SignupRequest>() }.getOrNull()
                ?.let { accounts.register(it.email, "${it.firstName} ${it.lastName}") }
        },
        onUnauthenticated = { Response(SEE_OTHER).location(Uri.of("/?next=${it.uri.path}")) }
    )

    val app = ServerFilters.CatchAll()
        .then(
            routes(
                "/passkeys" bind passkeys.routes,
                "/login/password" bind POST to passwordLogin(accounts, session),
                "/logout" bind GET to passkeys.logout,
                "/signup" bind GET to { Response(OK).with(html of SignupView) },
                "/protected" bind GET to passkeys.authFilter.then {
                    Response(OK).with(html of ProtectedView(accounts.displayNameOf(contextKey(it))))
                },
                "/" bind GET to { Response(OK).with(html of LoginView) }
            )
        )

    app.asServer(SunHttp(9000)).start().also { println("passkeys demo: http://localhost:9000") }.block()
}

private fun passwordLogin(accounts: Accounts, session: Principals) = { request: Request ->
    val form = loginForm(request)
    when (val handle = accounts.login(username(form), password(form))) {
        null -> Response(SEE_OTHER).location(Uri.of("/?error"))
        else -> session.write(handle, Response(SEE_OTHER).location(Uri.of("/protected")))
    }
}
