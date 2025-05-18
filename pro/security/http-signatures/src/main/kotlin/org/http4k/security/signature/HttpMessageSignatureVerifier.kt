package org.http4k.security.signature

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.lens.Header
import org.http4k.lens.SIGNATURE
import org.http4k.security.Nonce
import org.http4k.security.signature.SignatureBaseCreator.Companion.Default
import org.http4k.security.signature.VerificationError.InvalidExpiration
import org.http4k.security.signature.VerificationError.InvalidNonce
import org.http4k.security.signature.VerificationError.InvalidSignature
import org.http4k.security.signature.VerificationError.InvalidSignatureHeader
import org.http4k.security.signature.VerificationError.MissingHeader
import org.http4k.security.signature.VerificationError.MissingRequiredComponents
import org.http4k.security.signature.VerificationError.SignatureBaseCreationError
import org.http4k.security.signature.VerificationError.SignatureLabelNotFound
import org.http4k.security.signature.VerificationError.UnknownKeyId
import org.http4k.security.signature.VerificationError.UnsupportedAlgorithm
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Verifies the signature on a message. If signatureLabel is provided, verifies only that specific signature.
 * Otherwise, verifies the first signature found.
 */
class HttpMessageSignatureVerifier<Target : HttpMessage, PrivateKey, PublicKey>(
    private val componentsFactory: SignatureComponentFactory<Target>,
    private val keyResolver: (KeyId) -> PublicKey?,
    private val algorithm: SignatureAlgorithm<PrivateKey, PublicKey>,
    private val requiredComponents: List<SignatureComponent<Target>>,
    private val clock: Clock = Clock.systemUTC(),
    private val nonceVerifier: (Nonce, Target) -> Boolean = { _, _ -> true },
    private val signatureBaseCreator: SignatureBaseCreator<Target> = Default(),
    private val signatureLabel: String? = null,
    private val clockSkew: Duration = Duration.ofSeconds(30)
) {
    operator fun invoke(request: Request, target: Target): Result<Unit, VerificationError> {
        val signatureInput = SignatureInputParser(componentsFactory)(target)
            ?: return Failure(MissingHeader("Signature-Input"))

        val parsedSignatures = Header.SIGNATURE(target) ?: return Failure(MissingHeader("Signature"))

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

        val publicKey = keyResolver(input.parameters.keyId)
            ?: return Failure(UnknownKeyId(input.parameters.keyId))

        val now = Instant.now(clock)

        input.parameters.created?.let {
            if (it.isAfter(now + clockSkew)) return Failure(InvalidExpiration("Signature created in the future"))
        }

        input.parameters.expires?.let {
            if (it.isBefore(now + clockSkew)) return Failure(
                InvalidExpiration(
                    "Signature expired"
                )
            )
        }

        input.parameters.nonce?.let {
            if (!nonceVerifier(
                    it,
                    target
                )
            ) return Failure(InvalidNonce(it))
        }

        return when (val signatureBaseResult =
            signatureBaseCreator(request, target, input.components, input.parameters)) {
            is Success -> when {
                algorithm.verify(signatureBaseResult.value, matchingSignature.value, publicKey) -> Success(Unit)
                else -> Failure(InvalidSignature)
            }

            is Failure -> Failure(SignatureBaseCreationError(signatureBaseResult.reason))
        }
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
