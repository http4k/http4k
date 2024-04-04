package org.http4k.lens

val Header.AUTHORIZATION_BASIC get() = basicCredentials().optional("Authorization")

