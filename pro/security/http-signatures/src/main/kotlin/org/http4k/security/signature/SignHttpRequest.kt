package org.http4k.security.signature

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.filter.ClientFilters
import org.http4k.security.Nonce
import org.http4k.security.signature.SignatureError.SignatureBaseCreationError
import org.http4k.security.signature.SignatureError.SigningError
import java.time.Clock
import java.time.Duration
import java.time.Instant

fun <PrivateKey, PublicKey> ClientFilters.SignHttpRequest(
    components: List<SignatureComponent<Request>>,
    keyId: KeyId,
    privateKey: PrivateKey,
    algorithm: SignatureAlgorithm<PrivateKey, PublicKey>,
    clock: Clock = Clock.systemUTC(),
    nonce: Nonce? = null,
    expiry: Duration? = null,
    tag: String? = null,
    signatureLabel: String = "sig1",
    signatureBaseCreator: SignatureBaseCreator<Request> = SignatureBaseCreator.Default(),
): Filter = Filter { next ->
    { req ->
        val created = Instant.now(clock)
        val expires = expiry?.let { created + it }

        val params = SignatureParameters(keyId, algorithm.name, created, expires, nonce, tag)

        val signatureResult = signatureBaseCreator(req, components, params)
            .mapFailure(::SignatureBaseCreationError)
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
                    nonce?.let { add("nonce=\"$it\"") }
                    tag?.let { add("tag=\"$it\"") }
                }.joinToString(";")

                Signature(signatureLabel, signature) to paramsList
            }

        when (signatureResult) {
            is Success -> {
                val (signature, paramsList) = signatureResult.value

                next(
                    req
                        .header("Signature-Input", "${signature.label}=$paramsList")
                        .header("Signature", "${signature.label}=${signature.value}")
                )
            }

            is Failure -> throw IllegalStateException("Failed to create signature: ${signatureResult.reason}")
        }
    }
}

sealed class SignatureError {
    data class SignatureBaseCreationError(val error: ExtractorError) : SignatureError()
    data class SigningError(val message: String) : SignatureError()
}
