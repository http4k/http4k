/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.model.Base64UriBlob
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.Uri
import org.http4k.lens.location
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.passkeys.Principal.Known
import org.http4k.security.passkeys.model.AuthenticationOptions
import org.http4k.security.passkeys.model.AuthenticationResponse
import org.http4k.security.passkeys.model.CredentialDescriptor
import org.http4k.security.passkeys.model.PasskeyUser
import org.http4k.security.passkeys.model.PendingCeremony
import org.http4k.security.passkeys.model.RegistrationOptions
import org.http4k.security.passkeys.model.RegistrationResponse
import org.http4k.security.passkeys.model.RelyingParty
import org.http4k.security.passkeys.model.WebAuthnPolicy
import org.http4k.security.passkeys.util.AuthenticationResult
import org.http4k.security.passkeys.util.PasskeysJson.json

/**
 * Infrastructure for Passkey support. Operates in one of 2 modes based on if the end system needs
 * truly passwordless or has passkeys as an add-on option
 */
class Passkeys private constructor(
    private val rp: RelyingParty,
    private val verifier: PasskeyVerifier,
    private val persistence: PasskeyPersistence,
    private val principals: Principals,
    private val userFor: (Request) -> PasskeyUser?,
    private val onUnauthenticated: (Request) -> Response,
    private val policy: WebAuthnPolicy,
    private val allowedCredentialsFor: (Request) -> List<CredentialDescriptor>,
    private val newChallenge: () -> Base64UriBlob
) {
    private fun registrationOptions(challenge: Base64UriBlob, user: PasskeyUser, exclude: List<CredentialDescriptor>) =
        RegistrationOptions(
            rp, challenge, user, exclude,
            policy.pubKeyCredParams, policy.authenticatorSelection, policy.attestation, policy.timeout
        )

    private fun authenticationOptions(challenge: Base64UriBlob, allow: List<CredentialDescriptor>) =
        AuthenticationOptions(rp, challenge, allow, policy.authenticatorSelection.userVerification, policy.timeout)

    private val registerOptions: HttpHandler = { request ->
        when (val user = userFor(request)) {
            null -> Response(UNAUTHORIZED)
            else -> {
                val challenge = newChallenge()
                val body = registrationOptions(challenge, user, persistence.findByUser(user.handle))
                persistence.assignPending(Response(OK).json(body), PendingCeremony(challenge, user))
            }
        }
    }

    private val register: HttpHandler = { request ->
        val pending = persistence.retrievePending(request)
        val response = when {
            pending?.user == null -> Response(BAD_REQUEST)
            else -> {
                val options = registrationOptions(pending.challenge, pending.user, emptyList())
                when (val result = verifier.verifyRegistration(options, request.json<RegistrationResponse>())) {
                    is Success -> principals.write(result.value.userHandle, Response(OK))
                        .also { persistence.save(result.value) }
                    is Failure -> Response(BAD_REQUEST)
                }
            }
        }
        persistence.clearPending(response)
    }

    private val authenticateOptions: HttpHandler = { request ->
        val challenge = newChallenge()
        persistence.assignPending(
            Response(OK).json(authenticationOptions(challenge, allowedCredentialsFor(request))),
            PendingCeremony(challenge, null)
        )
    }

    private val authenticate: HttpHandler = { request ->
        val pending = persistence.retrievePending(request)
        val response = when {
            pending == null -> Response(BAD_REQUEST)
            else -> {
                val response = request.json<AuthenticationResponse>()
                val stored = persistence.findById(response.credentialId)
                when {
                    stored == null -> Response(UNAUTHORIZED)
                    else ->
                        when (val result = verifier.verifyAuthentication(
                            authenticationOptions(pending.challenge, emptyList()), response, stored
                        )) {
                            is Success -> principals.write(
                                stored.userHandle,
                                Response(OK).json(AuthenticationResult(stored.userHandle))
                            ).also {
                                persistence.save(
                                    stored.copy(
                                        signCount = result.value.signCount,
                                        backupState = result.value.backupState
                                    )
                                )
                            }

                            is Failure -> Response(UNAUTHORIZED)
                        }
                }
            }
        }
        persistence.clearPending(response)
    }

    val authFilter = Filter { next ->
        {
            when (val result = principals.read(it)) {
                is Known -> next(result.request)
                else -> onUnauthenticated(it)
            }
        }
    }

    val logout: HttpHandler = { principals.clear(Response(SEE_OTHER).location(Uri.of("/"))) }

    val routes = routes(
        "register/options" bind POST to registerOptions,
        "register" bind POST to register,
        "authenticate/options" bind POST to authenticateOptions,
        "authenticate" bind POST to authenticate
    )

    companion object {
        /**
         * Add-a-passkey mode: registration requires an existing session to add a passkey
         */
        fun onTopOfExistingLogin(
            rp: RelyingParty,
            verifier: PasskeyVerifier,
            persistence: PasskeyPersistence,
            principals: Principals,
            user: (Base64UriBlob) -> PasskeyUser?,
            onUnauthenticated: (Request) -> Response = { Response(UNAUTHORIZED) },
            policy: WebAuthnPolicy = WebAuthnPolicy(),
            allowedCredentialsFor: (Request) -> List<CredentialDescriptor> = { emptyList() },
            newChallenge: () -> Base64UriBlob = { Base64UriBlob.randomChallenge() }
        ) = Passkeys(
            rp, verifier, persistence, principals,
            {
                when (val read = principals.read(it)) {
                    is Known -> user(read.userHandle)
                    else -> null
                }
            }, onUnauthenticated, policy, allowedCredentialsFor, newChallenge
        )

        /**
         * Passkey-only mode: App resolves the registering user from the request - e.g. a signup body - so
         * a brand-new user can create an account with a passkey and no prior session.
         */
        fun passwordless(
            rp: RelyingParty,
            verifier: PasskeyVerifier,
            persistence: PasskeyPersistence,
            principals: Principals,
            user: (Request) -> PasskeyUser?,
            onUnauthenticated: (Request) -> Response = { Response(UNAUTHORIZED) },
            policy: WebAuthnPolicy = WebAuthnPolicy(),
            allowedCredentialsFor: (Request) -> List<CredentialDescriptor> = { emptyList() },
            newChallenge: () -> Base64UriBlob = { Base64UriBlob.randomChallenge() }
        ) = Passkeys(
            rp, verifier, persistence, principals, user,
            onUnauthenticated, policy, allowedCredentialsFor, newChallenge
        )
    }
}
