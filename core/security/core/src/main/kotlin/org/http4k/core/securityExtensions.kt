package org.http4k.core

import org.http4k.security.Security

fun HttpFilter(security: Security): Filter = Filter(security)

fun Filter(security: Security): Filter = Filter { next -> { security.filter(next)(it) } }
