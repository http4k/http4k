/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys.webauthn4j

import com.webauthn4j.WebAuthnManager
import com.webauthn4j.converter.AttestationObjectConverter
import com.webauthn4j.converter.exception.DataConversionException
import com.webauthn4j.converter.util.ObjectConverter
import com.webauthn4j.credential.CredentialRecordImpl
import com.webauthn4j.data.AuthenticationParameters
import com.webauthn4j.data.AuthenticationRequest
import com.webauthn4j.data.PublicKeyCredentialParameters
import com.webauthn4j.data.PublicKeyCredentialType.PUBLIC_KEY
import com.webauthn4j.data.RegistrationParameters
import com.webauthn4j.data.RegistrationRequest
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.server.ServerProperty
import com.webauthn4j.verifier.exception.BadChallengeException
import com.webauthn4j.verifier.exception.BadOriginException
import com.webauthn4j.verifier.exception.BadRpIdException
import com.webauthn4j.verifier.exception.BadSignatureException
import com.webauthn4j.verifier.exception.MaliciousCounterValueException
import com.webauthn4j.verifier.exception.VerificationException
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.connect.model.Base64UriBlob
import org.http4k.security.passkeys.PasskeyVerifier
import org.http4k.security.passkeys.model.AuthenticationOptions
import org.http4k.security.passkeys.model.AuthenticationResponse
import org.http4k.security.passkeys.model.AuthenticationVerdict
import org.http4k.security.passkeys.model.PasskeyError
import org.http4k.security.passkeys.model.PasskeyError.BadSignature
import org.http4k.security.passkeys.model.PasskeyError.ChallengeMismatch
import org.http4k.security.passkeys.model.PasskeyError.CounterRegression
import org.http4k.security.passkeys.model.PasskeyError.OriginMismatch
import org.http4k.security.passkeys.model.PasskeyError.Unknown
import org.http4k.security.passkeys.model.RegisteredCredential
import org.http4k.security.passkeys.model.RegistrationOptions
import org.http4k.security.passkeys.model.RegistrationResponse
import org.http4k.security.passkeys.model.RelyingParty
import org.http4k.security.passkeys.model.UserVerification.REQUIRED

/**
 * A [PasskeyVerifier] backed by webauthn4j: decodes CBOR/COSE, checks the challenge, origin, rpId hash,
 * signature and the sign counter (clone detection), and enforces the server-issued [UserVerification].
 */
class WebAuthn4jPasskeyVerifier(
    objectConverter: ObjectConverter = ObjectConverter(),
    private val manager: WebAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager(objectConverter)
) : PasskeyVerifier {
    private val attestationObjects = AttestationObjectConverter(objectConverter)

    override fun verifyRegistration(
        options: RegistrationOptions,
        response: RegistrationResponse
    ): Result<RegisteredCredential, PasskeyError> = try {
        val data = manager.verify(
            RegistrationRequest(response.attestationObject.decodedBytes(), response.clientDataJSON.decodedBytes()),
            RegistrationParameters(
                serverProperty(options.rp, options.challenge),
                options.pubKeyCredParams.map {
                    PublicKeyCredentialParameters(PUBLIC_KEY, COSEAlgorithmIdentifier.create(it.alg))
                },
                options.authenticatorSelection.userVerification == REQUIRED,
                true
            )
        )
        // webauthn4j guarantees these are present once verify() returns without throwing; assert the invariant once
        val attestationObject = requireNotNull(data.attestationObject) { "verified registration has no attestation object" }
        val authenticatorData = attestationObject.authenticatorData
        val attestedCredentialData =
            requireNotNull(authenticatorData.attestedCredentialData) { "verified registration has no attested credential data" }
        Success(
            RegisteredCredential(
                credentialId = Base64UriBlob.encode(attestedCredentialData.credentialId),
                publicKey = Base64UriBlob.encode(attestationObjects.convertToBytes(attestationObject)),
                signCount = authenticatorData.signCount,
                userHandle = options.user.handle,
                backupEligible = authenticatorData.isFlagBE,
                backupState = authenticatorData.isFlagBS,
                transports = response.transports,
                discoverable = response.clientExtensionResults.credProps?.rk
            )
        )
    } catch (e: VerificationException) {
        Failure(e.toPasskeyError())
    } catch (_: DataConversionException) {
        Failure(Unknown)
    } catch (_: IllegalArgumentException) {
        Failure(Unknown)
    }

    override fun verifyAuthentication(
        options: AuthenticationOptions,
        response: AuthenticationResponse,
        stored: RegisteredCredential
    ): Result<AuthenticationVerdict, PasskeyError> = try {
        val storedCredential = requireNotNull(attestationObjects.convert(stored.publicKey.decodedBytes())) {
            "stored credential public key is not a valid attestation object"
        }
        // the attestation object only carries the registration-time counter; inject the latest persisted
        // value so webauthn4j's clone detection compares against it rather than the stale baseline
        val credentialRecord = CredentialRecordImpl(storedCredential, null, null, null)
            .apply { counter = stored.signCount }
        val data = manager.verify(
            AuthenticationRequest(
                response.credentialId.decodedBytes(),
                response.authenticatorData.decodedBytes(),
                response.clientDataJSON.decodedBytes(),
                response.signature.decodedBytes()
            ),
            AuthenticationParameters(
                serverProperty(options.rp, options.challenge),
                credentialRecord,
                options.allowCredentials.takeIf { it.isNotEmpty() }?.map { it.id.decodedBytes() },
                options.userVerification == REQUIRED,
                true
            )
        )
        val authenticatorData = requireNotNull(data.authenticatorData) { "verified assertion has no authenticator data" }
        Success(AuthenticationVerdict(authenticatorData.signCount, authenticatorData.isFlagBS))
    } catch (e: VerificationException) {
        Failure(e.toPasskeyError())
    } catch (_: DataConversionException) {
        Failure(Unknown)
    } catch (_: IllegalArgumentException) {
        Failure(Unknown)
    }

    private fun serverProperty(rp: RelyingParty, challenge: Base64UriBlob) =
        ServerProperty.builder()
            .origins(rp.origins.map { Origin(it.toString()) }.toSet())
            .rpId(rp.id)
            .challenge(DefaultChallenge(challenge.decodedBytes()))
            .build()

    private fun VerificationException.toPasskeyError() = when (this) {
        is BadChallengeException -> ChallengeMismatch
        is BadOriginException, is BadRpIdException -> OriginMismatch
        is BadSignatureException -> BadSignature
        is MaliciousCounterValueException -> CounterRegression
        else -> Unknown
    }
}
