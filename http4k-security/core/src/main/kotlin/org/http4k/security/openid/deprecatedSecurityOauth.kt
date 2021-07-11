package org.http4k.security.openid

@Deprecated("moved to org.http4k.security", ReplaceWith("org.http4k.security.Nonce"))
typealias Nonce = org.http4k.security.Nonce

@Deprecated("moved to org.http4k.security", ReplaceWith("org.http4k.security.NonceGenerator"))
typealias NonceGenerator = () -> org.http4k.security.Nonce
