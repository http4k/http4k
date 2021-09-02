package org.http4k.security.oauth.client

import java.time.Instant

data class TokenData(val accessToken: String, val expiresAt: Instant?)
