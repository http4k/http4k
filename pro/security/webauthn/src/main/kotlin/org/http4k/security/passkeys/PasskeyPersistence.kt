/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys

import org.http4k.connect.model.Base64UriBlob
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.security.passkeys.model.CredentialDescriptor
import org.http4k.security.passkeys.model.PendingCeremony
import org.http4k.security.passkeys.model.RegisteredCredential

/**
 * Storage for both the registered passkeys and the ephemeral per-ceremony challenge.
 */
interface PasskeyPersistence {
    fun save(credential: RegisteredCredential)
    fun findById(credentialId: Base64UriBlob): RegisteredCredential?
    fun findByUser(userHandle: Base64UriBlob): List<CredentialDescriptor>
    fun assignPending(response: Response, pending: PendingCeremony): Response
    fun retrievePending(request: Request): PendingCeremony?

    /** Invalidate the pending-ceremony cookie/state once it has been consumed (or was expired/absent). */
    fun clearPending(response: Response): Response
}
