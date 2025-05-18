package org.http4k.security.signature

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.HttpMessage
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.SIGNATURE
import org.http4k.security.NonceGenerator
import org.http4k.security.signature.SignatureBaseCreator.Companion.Default
import org.http4k.security.signature.SignatureError.SigningError
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Creates a signature for signature a message.
 */
class HttpMessageSigner<Target : HttpMessage, PrivateKey, PublicKey>(
    private val components: List<SignatureComponent<Target>>,
    private val keyId: KeyId,
    private val privateKey: PrivateKey,
    private val algorithm: SignatureAlgorithm<PrivateKey, PublicKey>,
    private val clock: Clock = Clock.systemUTC(),
    private val nonceGenerator: NonceGenerator? = null,
    private val expiry: Duration? = null,
    private val tag: String? = null,
    private val signatureLabel: String = "sig1",
    private val signatureBaseCreator: SignatureBaseCreator<Target> = Default(),
) {
    operator fun invoke(req: Request, signingTarget: Target): Result<Target, SignatureError> {
        val created = Instant.now(clock)
        val expires = expiry?.let { created + it }

        val params = SignatureParameters(keyId, algorithm.name, created, expires, nonceGenerator?.let { it() }, tag)

        return signatureBaseCreator(req, signingTarget, components, params)
            .mapFailure(SignatureError::SignatureBaseCreationError)
            .flatMap {
                try {
                    Success(algorithm.sign(it, privateKey))
                } catch (e: Exception) {
                    Failure(SigningError(e.message ?: "Unknown signing error"))
                }
            }
            .map { signature ->
                val componentsList = components.joinToString(" ") { "\"${formatComponentName(it)}\"" }

                val paramsList = buildList {
                    add("($componentsList)")
                    add("created=${created.epochSecond}")
                    expires?.let { add("expires=${it.epochSecond}") }
                    add("keyid=\"$keyId\"")
                    add("alg=\"${algorithm.name}\"")
                    nonceGenerator?.let { add("nonce=\"$it\"") }
                    tag?.let { add("tag=\"$it\"") }
                }.joinToString(";")

                @Suppress("UNCHECKED_CAST")
                signingTarget
                    .header("Signature-Input", "${signatureLabel}=$paramsList")
                    .with(Header.SIGNATURE of listOf(Signature(signatureLabel, signature)))
                    as Target
            }
    }
}

sealed class SignatureError {
    data class SignatureBaseCreationError(val error: ExtractorError) : SignatureError()
    data class SigningError(val message: String) : SignatureError()
}
