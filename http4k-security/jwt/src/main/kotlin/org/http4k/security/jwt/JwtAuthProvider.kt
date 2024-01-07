package org.http4k.security.jwt

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.proc.BadJOSEException
import com.nimbusds.jose.proc.JWSKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import com.nimbusds.jwt.proc.JWTClaimsSetVerifier
import com.nimbusds.jwt.proc.JWTProcessor
import java.text.ParseException
import java.time.Clock
import java.util.Date

fun interface JwtAuthProvider: (String) -> JWTClaimsSet?

/**
 * Build fully custom JWT Auth Provider
 */
fun JwtAuthProvider(
    processor: JWTProcessor<SecurityContext>,
    onParseFailure: (String, ParseException) -> Unit = { _, _ -> },
    onRejected: (String, BadJOSEException) -> Unit = { _, _ -> },
    onError: (String, JOSEException) -> Unit = { _, _ -> }
) = JwtAuthProvider { token ->
    try {
        processor.process(token, null)
    } catch (e: BadJOSEException){
        onRejected(token, e)
        null
    } catch (e: JOSEException) {
        onError(token, e)
        null
    } catch (e: ParseException) {
        onParseFailure(token, e)
        null
    }
}

/**
 * Build provider with the given verifier and key selector
 */
fun JwtAuthProvider(
    verifier: JWTClaimsSetVerifier<SecurityContext>,
    keySelector: JWSKeySelector<SecurityContext>,
    onParseFailure: (String, ParseException) -> Unit = { _, _ -> },
    onRejected: (String, BadJOSEException) -> Unit = { _, _ -> },
    onError: (String, JOSEException) -> Unit = { _, _ -> }
) = JwtAuthProvider(
    processor = DefaultJWTProcessor<SecurityContext>().apply {
        jwtClaimsSetVerifier = verifier
        jwsKeySelector = keySelector
    },
    onParseFailure = onParseFailure,
    onRejected = onRejected,
    onError = onError
)

/**
 * Build provider with the given selector and verification requirements.
 *
 * Expiry is determined with the given Clock.
 */
fun JwtAuthProvider(
    keySelector: JWSKeySelector<SecurityContext>,
    audience: Set<String> = emptySet(),
    exactMatchClaims: JWTClaimsSet? = null,
    requiredClaims: Set<String> = emptySet(),
    prohibitedClaims: Set<String> = emptySet(),
    clock: Clock = Clock.systemUTC(),
    onParseFailure: (String, ParseException) -> Unit = { _, _ -> },
    onRejected: (String, BadJOSEException) -> Unit = { _, _ -> },
    onError: (String, JOSEException) -> Unit = { _, _ -> }
) = JwtAuthProvider(
    verifier = object: DefaultJWTClaimsVerifier<SecurityContext>(
        audience,
        exactMatchClaims,
        requiredClaims,
        prohibitedClaims
    ) {
        override fun currentTime() = Date.from(clock.instant())
    },
    keySelector = keySelector,
    onParseFailure = onParseFailure,
    onRejected = onRejected,
    onError = onError
)
