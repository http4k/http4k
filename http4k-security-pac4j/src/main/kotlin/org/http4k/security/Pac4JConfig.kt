package org.http4k.security

import org.pac4j.core.config.Config

object Pac4JConfig {
    operator fun invoke(fn: Config.() -> Unit): Config = Config().apply {
        httpActionAdapter = Http4kHttpActionAdapter()
        fn()
    }
}