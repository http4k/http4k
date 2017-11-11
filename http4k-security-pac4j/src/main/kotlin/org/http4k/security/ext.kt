package org.http4k.security

import org.pac4j.core.config.Config

internal fun Config.http4kAdapter() = httpActionAdapter as Http4kHttpActionAdapter