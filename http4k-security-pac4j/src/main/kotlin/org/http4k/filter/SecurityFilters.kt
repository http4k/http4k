package org.http4k.filter

import org.http4k.core.Filter
import org.http4k.core.Response
import org.http4k.security.Http4kWebContext
import org.http4k.security.http4kAdapter
import org.pac4j.core.config.Config
import org.pac4j.core.engine.DefaultSecurityLogic
import org.pac4j.core.engine.SecurityGrantedAccessAdapter
import org.pac4j.core.exception.TechnicalException

object SecurityFilters {
    object Secure {
        private object SecurityGrantedAccessException : TechnicalException("access")

        operator fun invoke(
            config: Config, multiProfile: Boolean = false
        ) = DefaultSecurityLogic<Response, Http4kWebContext>().run {
            Filter { next ->
                {
                    try {
                        perform(Http4kWebContext(it, config.sessionStore), config,
                            SecurityGrantedAccessAdapter { _, _ -> throw SecurityGrantedAccessException },
                            config.http4kAdapter(), "", "", "", multiProfile)
                    } catch (e: SecurityGrantedAccessException) {
                        next(it)
                    }
                }
            }
        }
    }
}