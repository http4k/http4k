package org.http4k.security

import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Uri
import org.pac4j.core.config.Config
import org.pac4j.core.engine.DefaultCallbackLogic

object Callback {
    operator fun invoke(config: Config,
                        defaultUrl: Uri,
                        multiProfile: Boolean = false,
                        renewSession: Boolean = false): HttpHandler =
        DefaultCallbackLogic<Response, Http4kWebContext>().run {
            {
                perform(Http4kWebContext(it, config.sessionStore), config,
                    config.http4kAdapter(),
                    defaultUrl.toString(), multiProfile, renewSession)
            }
        }
}
