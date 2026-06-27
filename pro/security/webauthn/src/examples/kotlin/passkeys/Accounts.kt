/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package passkeys

import org.http4k.connect.model.Base64UriBlob
import org.http4k.security.passkeys.model.PasskeyUser
import org.http4k.security.passkeys.randomHandle

/**
 * Stands in for the app's account system
 */
class Accounts {
    private data class Account(val name: String, val handle: Base64UriBlob, val password: String?, val displayName: String)

    private val byName = mutableMapOf<String, Account>()

    /** Demo password login: creates the account on first use, then checks the password. */
    fun login(name: String, password: String): Base64UriBlob? {
        val account = byName.getOrPut(name) { Account(name, Base64UriBlob.randomHandle(), password, name) }
        return account.handle.takeIf { account.password == password }
    }

    /** Passwordless registration: create (or reuse) an account with no password - the passkey is its only credential. */
    fun register(name: String, displayName: String): PasskeyUser =
        byName.getOrPut(name) { Account(name, Base64UriBlob.randomHandle(), null, displayName) }
            .let { PasskeyUser(it.handle, it.name, it.displayName) }

    /** The logged-in user (from their session handle) for the add-a-passkey ceremony. */
    fun userForHandle(handle: Base64UriBlob): PasskeyUser? =
        byHandle(handle)?.let { PasskeyUser(it.handle, it.name, it.displayName) }

    fun displayNameOf(handle: Base64UriBlob): String? = byHandle(handle)?.displayName

    private fun byHandle(handle: Base64UriBlob) = byName.values.firstOrNull { it.handle == handle }
}
