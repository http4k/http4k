package org.http4k.security

import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Uri
import org.pac4j.core.config.Config
import org.pac4j.core.engine.DefaultLogoutLogic

object Logout {
    operator fun invoke(
        config: Config,
        defaultUrl: Uri,
        logoutUrlPattern: Uri): HttpHandler {
        val logoutLogic = DefaultLogoutLogic<Response, Http4kWebContext>()

        return {
            logoutLogic.perform(Http4kWebContext(it, config.sessionStore), config,
                config.http4kAdapter(),
                defaultUrl.toString(), logoutUrlPattern.toString(), false, false, false)
        }
    }
}