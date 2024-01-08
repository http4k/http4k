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

fun interface JwtAuthorizer<Principal: Any>: (String) -> Principal?

/**
 * Build fully custom JWT Auth Provider
 */
fun <Principal: Any> JwtAuthorizer(
    processor: JWTProcessor<SecurityContext>,
    lookup: (JWTClaimsSet) -> Principal?,
    onParseFailure: (String, ParseException) -> Unit = { _, _ -> },
    onRejected: (String, BadJOSEException) -> Unit = { _, _ -> },
    onError: (String, JOSEException) -> Unit = { _, _ -> }
) = JwtAuthorizer { token ->
    try {
        processor.process(token, null).let(lookup)
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
fun <Principal: Any> JwtAuthorizer(
    verifier: JWTClaimsSetVerifier<SecurityContext>,
    lookup: (JWTClaimsSet) -> Principal?,
    keySelector: JWSKeySelector<SecurityContext>,
    onParseFailure: (String, ParseException) -> Unit = { _, _ -> },
    onRejected: (String, BadJOSEException) -> Unit = { _, _ -> },
    onError: (String, JOSEException) -> Unit = { _, _ -> }
) = JwtAuthorizer(
    processor = DefaultJWTProcessor<SecurityContext>().apply {
        jwtClaimsSetVerifier = verifier
        jwsKeySelector = keySelector
    },
    lookup = lookup,
    onParseFailure = onParseFailure,
    onRejected = onRejected,
    onError = onError
)

/**
 * Build provider with the given selector and verification requirements.
 *
 * Expiry is determined with the given Clock.
 */
fun <Principal: Any> JwtAuthorizer(
    keySelector: JWSKeySelector<SecurityContext>,
    lookup: (JWTClaimsSet) -> Principal?,
    audience: Set<String>? = null,
    exactMatchClaims: JWTClaimsSet? = null,
    requiredClaims: Set<String> = emptySet(),
    prohibitedClaims: Set<String> = emptySet(),
    clock: Clock = Clock.systemUTC(),
    onParseFailure: (String, ParseException) -> Unit = { _, _ -> },
    onRejected: (String, BadJOSEException) -> Unit = { _, _ -> },
    onError: (String, JOSEException) -> Unit = { _, _ -> }
) = JwtAuthorizer(
    lookup = lookup,
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
