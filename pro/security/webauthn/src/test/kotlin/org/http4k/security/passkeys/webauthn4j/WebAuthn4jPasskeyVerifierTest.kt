/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.webauthn4j

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.webauthn4j.WebAuthnManager
import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.data.AuthenticatorSelectionCriteria
import com.webauthn4j.data.PublicKeyCredentialCreationOptions
import com.webauthn4j.data.PublicKeyCredentialParameters
import com.webauthn4j.data.PublicKeyCredentialRequestOptions
import com.webauthn4j.data.PublicKeyCredentialRpEntity
import com.webauthn4j.data.PublicKeyCredentialType.PUBLIC_KEY
import com.webauthn4j.data.PublicKeyCredentialUserEntity
import com.webauthn4j.data.ResidentKeyRequirement
import com.webauthn4j.data.UserVerificationRequirement
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.test.EmulatorUtil
import com.webauthn4j.test.authenticator.webauthn.PackedAuthenticator
import com.webauthn4j.test.authenticator.webauthn.WebAuthnAuthenticatorAdaptor
import com.webauthn4j.test.client.ClientPlatform
import com.webauthn4j.verifier.CustomRegistrationVerifier
import com.webauthn4j.verifier.attestation.statement.none.NoneAttestationStatementVerifier
import com.webauthn4j.verifier.attestation.trustworthiness.certpath.NullCertPathTrustworthinessVerifier
import com.webauthn4j.verifier.attestation.trustworthiness.self.NullSelfAttestationTrustworthinessVerifier
import com.webauthn4j.verifier.exception.BadAttestationStatementException
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.connect.model.Base64UriBlob
import org.http4k.core.Uri
import org.http4k.security.passkeys.model.AuthenticationOptions
import org.http4k.security.passkeys.model.AuthenticationResponse
import org.http4k.security.passkeys.model.PasskeyError
import org.http4k.security.passkeys.model.PasskeyUser
import org.http4k.security.passkeys.model.RegisteredCredential
import org.http4k.security.passkeys.model.RegistrationOptions
import org.http4k.security.passkeys.model.RegistrationResponse
import org.http4k.security.passkeys.model.RelyingParty
import org.http4k.security.passkeys.randomChallenge
import org.http4k.security.passkeys.randomHandle
import org.junit.jupiter.api.Test

class WebAuthn4jPasskeyVerifierTest {
    private val rpId = "example.com"
    private val rp = RelyingParty(rpId, "Example", Uri.of("https://example.com"))
    private val verifier = WebAuthn4jPasskeyVerifier()
    private val user = PasskeyUser(Base64UriBlob.randomHandle(), "alice", "Alice")

    private val client = ClientPlatform(Origin("https://example.com"), WebAuthnAuthenticatorAdaptor(PackedAuthenticator()))

    private fun register(options: RegistrationOptions, client: ClientPlatform = this.client): RegistrationResponse {
        val cred = client.create(
            PublicKeyCredentialCreationOptions(
                PublicKeyCredentialRpEntity(options.rp.id, options.rp.name),
                PublicKeyCredentialUserEntity(options.user.handle.decodedBytes(), options.user.name, options.user.displayName),
                DefaultChallenge(options.challenge.decodedBytes()),
                options.pubKeyCredParams.map {
                    PublicKeyCredentialParameters(PUBLIC_KEY, COSEAlgorithmIdentifier.create(it.alg))
                },
                null, null,
                AuthenticatorSelectionCriteria(null, ResidentKeyRequirement.PREFERRED, UserVerificationRequirement.PREFERRED),
                null, null
            )
        )
        return RegistrationResponse(
            Base64UriBlob.encode(cred.rawId!!),
            Base64UriBlob.encode(cred.response!!.clientDataJSON),
            Base64UriBlob.encode(cred.response!!.attestationObject),
            transports = cred.response!!.transports.orEmpty().map { it.value }
        )
    }

    private fun authenticate(options: AuthenticationOptions): AuthenticationResponse {
        val assertion = client.get(
            PublicKeyCredentialRequestOptions(
                DefaultChallenge(options.challenge.decodedBytes()), null, options.rp.id, null,
                UserVerificationRequirement.PREFERRED, null
            )
        )
        return AuthenticationResponse(
            Base64UriBlob.encode(assertion.rawId!!),
            Base64UriBlob.encode(assertion.response!!.clientDataJSON),
            Base64UriBlob.encode(assertion.response!!.authenticatorData),
            Base64UriBlob.encode(assertion.response!!.signature)
        )
    }

    private fun registerCredential(options: RegistrationOptions = RegistrationOptions(rp, Base64UriBlob.randomChallenge(), user)): RegisteredCredential =
        (verifier.verifyRegistration(options, register(options)) as Success).value

    @Test
    fun `verifies a real registration then authentication round trip`() {
        val credential = registerCredential()
        assertThat(credential.userHandle, equalTo(user.handle))

        val options = AuthenticationOptions(rp, Base64UriBlob.randomChallenge(), emptyList())
        val result = verifier.verifyAuthentication(options, authenticate(options), credential)

        assertThat(result is Success, equalTo(true))
    }

    @Test
    fun `captures backup flags and transports at registration, refreshes backup state on sign-in`() {
        val regOptions = RegistrationOptions(rp, Base64UriBlob.randomChallenge(), user)
        val response = register(regOptions)
        val credential = (verifier.verifyRegistration(regOptions, response) as Success).value

        assertThat(credential.transports, equalTo(response.transports))
        assertThat(credential.backupEligible, equalTo(false))
        assertThat(credential.backupState, equalTo(false))

        val authOptions = AuthenticationOptions(rp, Base64UriBlob.randomChallenge(), emptyList())
        val verdict = (verifier.verifyAuthentication(authOptions, authenticate(authOptions), credential) as Success).value
        assertThat(verdict.backupState, equalTo(credential.backupState))
    }

    @Test
    fun `rejects an assertion signed for a different origin`() {
        val credential = registerCredential()

        val wrongOrigin = RelyingParty(rpId, "Example", Uri.of("https://other.example.com"))
        val options = AuthenticationOptions(wrongOrigin, Base64UriBlob.randomChallenge(), emptyList())

        assertThat(verifier.verifyAuthentication(options, authenticate(options), credential), equalTo(Failure(PasskeyError.OriginMismatch)))
    }

    @Test
    fun `rejects a mismatched challenge`() {
        val credential = registerCredential()

        val signed = AuthenticationOptions(rp, Base64UriBlob.randomChallenge(), emptyList())
        val response = authenticate(signed)
        val different = AuthenticationOptions(rp, Base64UriBlob.randomChallenge(), emptyList())

        assertThat(verifier.verifyAuthentication(different, response, credential), equalTo(Failure(PasskeyError.ChallengeMismatch)))
    }

    @Test
    fun `rejects an unrelated credential being substituted`() {
        registerCredential()
        val somebodyElse = RegisteredCredential(
            Base64UriBlob.randomHandle(), Base64UriBlob.encode("not a key"), 0, Base64UriBlob.randomHandle()
        )
        val options = AuthenticationOptions(rp, Base64UriBlob.randomChallenge(), emptyList())

        assertThat(verifier.verifyAuthentication(options, authenticate(options), somebodyElse) is Failure, equalTo(true))
    }

    @Test
    fun `detects a regressed sign counter against the latest persisted value`() {
        val credential = registerCredential()

        val ahead = credential.copy(signCount = credential.signCount + 100)
        val options = AuthenticationOptions(rp, Base64UriBlob.randomChallenge(), emptyList())

        assertThat(
            verifier.verifyAuthentication(options, authenticate(options), ahead),
            equalTo(Failure(PasskeyError.CounterRegression))
        )
    }

    @Test
    fun `returns a failure rather than throwing on malformed base64 input`() {
        val credential = registerCredential()
        val options = AuthenticationOptions(rp, Base64UriBlob.randomChallenge(), emptyList())
        val malformed = authenticate(options).copy(signature = Base64UriBlob.of("!!!not-base64!!!"))

        assertThat(verifier.verifyAuthentication(options, malformed, credential), equalTo(Failure(PasskeyError.Unknown)))
    }

    @Test
    fun `uses the injected WebAuthnManager`() {
        val rejectingManager = WebAuthnManager(
            listOf(NoneAttestationStatementVerifier()),
            NullCertPathTrustworthinessVerifier(),
            NullSelfAttestationTrustworthinessVerifier(),
            listOf(CustomRegistrationVerifier { throw BadAttestationStatementException("rejected by injected manager") }),
            emptyList(),
            ObjectConverter()
        )
        val verifier = WebAuthn4jPasskeyVerifier(manager = rejectingManager)
        val noneClient = ClientPlatform(Origin("https://example.com"), WebAuthnAuthenticatorAdaptor(EmulatorUtil.NONE_ATTESTATION_AUTHENTICATOR))
        val options = RegistrationOptions(rp, Base64UriBlob.randomChallenge(), user)

        assertThat(verifier.verifyRegistration(options, register(options, noneClient)) is Failure, equalTo(true))
    }
}
