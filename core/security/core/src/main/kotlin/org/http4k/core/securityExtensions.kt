package org.http4k.core

import org.http4k.security.Security

fun Filter(security: Security): Filter = Filter { next ->
    {
        security.filter(next)(it)
    }
}
