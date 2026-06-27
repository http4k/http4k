/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.testing

import org.http4k.connect.model.Base64UriBlob
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.invalidateCookie
import org.http4k.security.passkeys.PasskeyPersistence
import org.http4k.security.passkeys.model.CredentialDescriptor
import org.http4k.security.passkeys.model.PendingCeremony
import org.http4k.security.passkeys.model.RegisteredCredential
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * NOT FOR PRODUCTION. In-memory store (lost on restart). Pending ceremonies expire after [ttl] (single-use
 * anyway); [clock] is injectable so tests can control expiry. Real deployments use a durable store with its
 * own TTL.
 */
// ponytail: in-memory; swap for a durable store in production.
class InMemoryPasskeyPersistence(
    private val ttl: Duration = Duration.ofMinutes(5),
    private val clock: Clock = Clock.systemUTC()
) : PasskeyPersistence {
    private val credentials = mutableMapOf<String, RegisteredCredential>()
    private val pending = mutableMapOf<String, Expiring>()

    private data class Expiring(val ceremony: PendingCeremony, val expiresAt: Instant)

    override fun save(credential: RegisteredCredential) {
        credentials[credential.credentialId.value] = credential
    }

    override fun findById(credentialId: Base64UriBlob) = credentials[credentialId.value]

    override fun findByUser(userHandle: Base64UriBlob) =
        credentials.values.filter { it.userHandle == userHandle }
            .map { CredentialDescriptor(it.credentialId, it.transports) }

    override fun assignPending(response: Response, pending: PendingCeremony) =
        UUID.randomUUID().toString().let { id ->
            this.pending[id] = Expiring(pending, clock.instant().plus(ttl))
            response.cookie(Cookie(PENDING, id, expires = clock.instant().plus(ttl), path = "/"))
        }

    override fun retrievePending(request: Request) =
        request.cookie(PENDING)?.value
            ?.let { pending.remove(it) }
            ?.takeIf { it.expiresAt.isAfter(clock.instant()) }
            ?.ceremony

    override fun clearPending(response: Response) = response.invalidateCookie(PENDING, path = "/")

    private companion object {
        const val PENDING = "http4k_passkey_pending"
    }
}
