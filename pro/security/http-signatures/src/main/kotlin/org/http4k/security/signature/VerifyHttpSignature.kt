package org.http4k.security.signature

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.Filter
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.filter.ServerFilters
import org.http4k.lens.Header
import org.http4k.security.Nonce
import org.http4k.security.signature.VerificationError.InvalidExpiration
import org.http4k.security.signature.VerificationError.InvalidNonce
import org.http4k.security.signature.VerificationError.InvalidSignature
import org.http4k.security.signature.VerificationError.InvalidSignatureHeader
import org.http4k.security.signature.VerificationError.MissingHeader
import org.http4k.security.signature.VerificationError.MissingRequiredComponents
import org.http4k.security.signature.VerificationError.OtherError
import org.http4k.security.signature.VerificationError.SignatureBaseCreationError
import org.http4k.security.signature.VerificationError.SignatureLabelNotFound
import org.http4k.security.signature.VerificationError.UnknownKeyId
import org.http4k.security.signature.VerificationError.UnsupportedAlgorithm
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Server filter that verifies HTTP signatures on incoming requests
 */
fun <PrivateKey, PublicKey> ServerFilters.VerifyHttpSignature(
    keyResolver: (KeyId) -> PublicKey?,
    algorithm: SignatureAlgorithm<PrivateKey, PublicKey>,
    requiredComponents: List<SignatureComponent<*>>,
    clock: Clock = Clock.systemUTC(),
    nonceVerifier: (Nonce, Request) -> Boolean = { _, _ -> true },
    signatureBaseCreator: SignatureBaseCreator<Request> = SignatureBaseCreator.Default(),
    signatureLabel: String? = null,
    componentsFactory: SignatureComponentFactory<Request> = SignatureComponentFactory.HttpRequest(),
    clockSkew: Duration = Duration.ofSeconds(30)
) = Filter { next ->
    { request ->
        verifySignature(
            request,
            keyResolver,
            requiredComponents,
            algorithm,
            signatureBaseCreator,
            clock,
            nonceVerifier,
            signatureLabel,
            componentsFactory,
            clockSkew
        )
            .map { next(request) }
            .mapFailure {
                when (it) {
                    is InvalidSignatureHeader -> Response(BAD_REQUEST)
                    is SignatureBaseCreationError -> Response(BAD_REQUEST)
                    is OtherError -> Response(BAD_REQUEST)
                    is MissingHeader -> Response(UNAUTHORIZED)
                    is MissingRequiredComponents -> Response(UNAUTHORIZED)
                    is UnsupportedAlgorithm -> Response(UNAUTHORIZED)
                    is UnknownKeyId -> Response(UNAUTHORIZED)
                    is InvalidExpiration -> Response(UNAUTHORIZED)
                    is InvalidNonce -> Response(UNAUTHORIZED)
                    is InvalidSignature -> Response(UNAUTHORIZED)
                    is SignatureLabelNotFound -> Response(UNAUTHORIZED)
                }
            }
            .get()
    }
}

/**
 * Verifies the signature on a request. If signatureLabel is provided, verifies only that specific signature.
 * Otherwise, verifies the first signature found.
 */
private fun <PrivateKey, PublicKey, M : HttpMessage> verifySignature(
    message: M,
    keyResolver: (KeyId) -> PublicKey?,
    requiredComponents: List<SignatureComponent<*>>,
    algorithm: SignatureAlgorithm<PrivateKey, PublicKey>,
    signatureBaseCreator: SignatureBaseCreator<M>,
    clock: Clock,
    nonceVerifier: (Nonce, M) -> Boolean,
    signatureLabel: String?,
    componentsFactory: SignatureComponentFactory<M>,
    clockSkew: Duration = Duration.ofSeconds(30)
): Result<Unit, VerificationError> {
    val signatureInput = SignatureInputParser(componentsFactory)(message)
        ?: return Failure(MissingHeader("Signature-Input"))

    val parsedSignatures = Header.SIGNATURE(message) ?: return Failure(MissingHeader("Signature"))

    if (parsedSignatures.isEmpty() || signatureInput.isEmpty()) {
        return Failure(InvalidSignatureHeader("Empty or invalid headers"))
    }

    val input = when (signatureLabel) {
        null -> signatureInput.firstOrNull()
            ?: return Failure(InvalidSignatureHeader("No valid signature inputs found"))

        else -> signatureInput.find { it.label == signatureLabel }
            ?: return Failure(SignatureLabelNotFound(signatureLabel))
    }

    val matchingSignature = parsedSignatures.find { it.label == input.label }
        ?: return Failure(InvalidSignatureHeader("No matching signature found for input: ${input.label}"))

    val missingComponents = requiredComponents.filter { it.name !in input.components.map { it.name } }
    if (missingComponents.isNotEmpty()) {
        return Failure(MissingRequiredComponents(missingComponents))
    }

    if (input.parameters.algorithm.isEmpty()) {
        return Failure(UnsupportedAlgorithm("Missing algorithm"))
    }

    if (input.parameters.algorithm != algorithm.name) {
        return Failure(
            UnsupportedAlgorithm(
                "Algorithm mismatch: expected ${algorithm.name}, got ${input.parameters.algorithm}"
            )
        )
    }

    val publicKey = keyResolver(input.parameters.keyId) ?: return Failure(UnknownKeyId(input.parameters.keyId))

    val now = Instant.now(clock)

    input.parameters.created?.let {
        if (it.isAfter(now + clockSkew)) return Failure(InvalidExpiration("Signature created in the future"))
    }

    input.parameters.expires?.let { if (it.isBefore(now + clockSkew)) return Failure(InvalidExpiration("Signature expired")) }

    input.parameters.nonce?.let { if (!nonceVerifier(it, message)) return Failure(InvalidNonce(it)) }

    return when (val signatureBaseResult = signatureBaseCreator(message, input.components, input.parameters)) {
        is Success -> when {
            algorithm.verify(signatureBaseResult.value, matchingSignature.value, publicKey) -> Success(Unit)
            else -> Failure(InvalidSignature)
        }

        is Failure -> Failure(SignatureBaseCreationError(signatureBaseResult.reason))
    }
}

sealed interface VerificationError {
    data class MissingHeader(val headerName: String) : VerificationError
    data class InvalidSignatureHeader(val reason: String) : VerificationError
    data class MissingRequiredComponents(val missingComponents: List<SignatureComponent<*>>) : VerificationError
    data class UnsupportedAlgorithm(val algorithm: String) : VerificationError
    data class UnknownKeyId(val keyId: String) : VerificationError
    data class InvalidExpiration(val reason: String) : VerificationError
    data class InvalidNonce(val nonce: Nonce) : VerificationError
    data class SignatureLabelNotFound(val label: String) : VerificationError
    data object InvalidSignature : VerificationError
    data class SignatureBaseCreationError(val error: ExtractorError) : VerificationError
    data class OtherError(val message: String, val exception: Exception? = null) : VerificationError
}
