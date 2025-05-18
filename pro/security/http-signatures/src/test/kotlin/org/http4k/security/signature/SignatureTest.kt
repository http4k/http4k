package org.http4k.security.signature

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Header
import org.http4k.lens.SIGNATURE
import org.http4k.security.Nonce
import org.http4k.security.signature.ExtractorError.UnsupportedComponent
import org.http4k.security.signature.SignatureComponent.Authority
import org.http4k.security.signature.SignatureComponent.Path
import org.http4k.security.signature.SignatureComponent.Query
import org.http4k.security.signature.SignatureComponent.QueryParam
import org.http4k.security.signature.SignatureComponent.RequestTarget
import org.http4k.security.signature.SignatureComponent.Scheme
import org.http4k.security.signature.SignatureComponent.TargetUri
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.time.Instant

class SignatureTest {

    @Test
    fun `can create signature base`() {
        val request = Request(POST, "https://example.com/api/resource")
            .header("Host", "example.com")
            .header("Content-Type", "application/json")
            .body("""{"hello": "world"}""")

        val components = listOf(
            SignatureComponent.Method,
            Path,
            Authority,
            SignatureComponent.Header("content-type")
        )

        val fixedTime = Instant.parse("2023-01-01T12:00:00Z")

        val params = SignatureParameters(
            keyId = "test-key-id",
            algorithm = "rsa-pss-sha512",
            created = fixedTime
        )

        val signatureBaseResult = SignatureBaseCreator.Default<Request>().invoke(
            request,
            request,
            components,
            params
        )
        assertTrue(signatureBaseResult is Success)
        val signatureBase = (signatureBaseResult as Success).value

        assertTrue("\"@method\": POST" in signatureBase)
        assertTrue("\"@path\": /api/resource" in signatureBase)
        assertTrue("\"@authority\": example.com" in signatureBase)
        assertTrue("\"content-type\": application/json" in signatureBase)
        assertTrue("\"@signature-params\": (\"@method\" \"@path\" \"@authority\" \"content-type\")" in signatureBase)
        assertTrue(";created=1672574400" in signatureBase)
        assertTrue(";keyid=\"test-key-id\"" in signatureBase)
        assertTrue(";alg=\"rsa-pss-sha512\"" in signatureBase)
    }

    @Test
    fun `can sign and verify signature`() {
        val keyPair = generateRsaKeyPair()

        val signatureBase =
            "\"@method\": GET\n\"@signature-params\": (\"@method\");created=1672574400;keyid=\"test-key-id\";alg=\"rsa-pss-sha512\""

        val signature = RsaPssSha512.sign(signatureBase, keyPair.private)

        assertTrue(signature.startsWith(":"))
        assertTrue(signature.endsWith(":"))

        val isValid = RsaPssSha512.verify(signatureBase, signature, keyPair.public)
        assertTrue(isValid, "Signature should be valid")

        val anotherKeyPair = generateRsaKeyPair()
        val isInvalid = RsaPssSha512.verify(signatureBase, signature, anotherKeyPair.public)
        assertTrue(!isInvalid, "Signature should be invalid with different key")
    }

    @Test
    fun `can parse signature input header for a request`() {
        val headerValue =
            "sig1=(\"@method\" \"@path\" \"content-type\");created=1618884473;keyid=\"test-key-rsa\";alg=\"rsa-pss-sha512\""

        val input = SignatureInputParser(SignatureComponentFactory.HttpRequest())(
            Request(GET, "").header(
                "Signature-Input",
                headerValue
            )
        )!!

        assertEquals(1, input.size)
        assertEquals("sig1", input[0].label)
        assertEquals(3, input[0].components.size)
        assertEquals("test-key-rsa", input[0].parameters.keyId)
        assertEquals("rsa-pss-sha512", input[0].parameters.algorithm)
        assertEquals(1618884473L, input[0].parameters.created?.epochSecond)
    }

    @Test
    fun `can parse signature input header for a response`() {
        val headerValue =
            "sig1=(\"@status\" \"content-type\");created=1618884473;keyid=\"test-key-rsa\";alg=\"rsa-pss-sha512\""

        val input = SignatureInputParser(SignatureComponentFactory.HttpResponse())(
            Response(OK).header(
                "Signature-Input",
                headerValue
            )
        )!!

        assertThat(
            input, equalTo(
                listOf(
                    SignatureInput(
                        "sig1",
                        listOf(SignatureComponent.Status, SignatureComponent.Header("content-type")),
                        SignatureParameters(
                            "test-key-rsa",
                            "rsa-pss-sha512",
                            Instant.ofEpochSecond(1618884473L),
                            null,
                            null,
                            null
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `can parse signature header`() {
        val headerValue = "sig1=:abc123:,sig2=:def456:"

        val parsedSignatures = Header.SIGNATURE(
            Request(GET, "")
                .header("Signature", headerValue)
        )!!

        assertEquals(2, parsedSignatures.size)
        assertEquals("sig1", parsedSignatures[0].label)
        assertEquals(":abc123:", parsedSignatures[0].value)
        assertEquals("sig2", parsedSignatures[1].label)
        assertEquals(":def456:", parsedSignatures[1].value)
    }

    @Test
    fun `correctly handles query parameters with empty values`() {
        val request = Request(POST, "https://example.com/api/resource?param=")
            .header("Host", "example.com")

        val component = QueryParam("param")
        val result = component(request, request)

        assertTrue(result is Success)
        assertEquals("", (result as Success).value)
    }

    @Test
    fun `rejects duplicate query parameters per RFC`() {
        val request = Request(POST, "https://example.com/api/resource?param=value1&param=value2")
            .header("Host", "example.com")

        val component = QueryParam("param")
        val result = component(request, request)

        assertTrue(result is Failure)
        assertTrue((result as Failure).reason is UnsupportedComponent)
    }

    @Test
    fun `comprehensive test demonstrating all components`() {
        val request = Request(POST, "https://api.example.com:8443/users/123?filter=active&sort=name&page=1&limit=10")
            .header("Host", "api.example.com")
            .header("Date", "Wed, 01 Nov 2023 12:30:45 GMT")
            .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
            .header("Content-Type", "application/json")
            .header("Content-Length", "57")
            .header("Content-Digest", "sha-256=:base64encodeddigest:")
            .header("Cache-Control", "no-cache, max-age=0")
            .header("Accept", "application/json")
            .header("X-Request-ID", "f058ebd6-02f7-4d3f-942e-904344e8cde5")
            .body("""{"name": "John Doe", "email": "john@example.com", "active": true}""")

        val allComponents = listOf(
            SignatureComponent.Method,
            Path,
            Query,
            Authority,
            Scheme,
            TargetUri,
            RequestTarget,

            QueryParam("filter"),
            QueryParam("sort"),
            QueryParam("page"),
            QueryParam("limit"),

            SignatureComponent.Header("host"),
            SignatureComponent.Header("date"),
            SignatureComponent.Header("authorization"),
            SignatureComponent.Header("content-type"),
            SignatureComponent.Header("content-length"),
            SignatureComponent.Header("content-digest"),
            SignatureComponent.Header("cache-control"),
            SignatureComponent.Header("accept"),
            SignatureComponent.Header("x-request-id")
        )

        val fixedCreationTime = Instant.parse("2023-01-01T12:00:00Z")
        val fixedExpirationTime = fixedCreationTime.plusSeconds(300)

        val signatureParams = SignatureParameters(
            keyId = "test-key-rsa-2048",
            algorithm = "rsa-pss-sha512",
            created = fixedCreationTime,
            expires = fixedExpirationTime,
            nonce = Nonce("abc123xyz789"),
            tag = "api-gateway"
        )

        val signatureBaseResult = SignatureBaseCreator.Default<Request>()
            .invoke(request, request, allComponents, signatureParams)

        assertTrue(
            signatureBaseResult is Success,
            "Signature base creation should succeed"
        )

        val signatureBase = (signatureBaseResult as Success).value

        assertTrue("\"@method\": POST" in signatureBase)
        assertTrue("\"@path\": /users/123" in signatureBase)
        assertTrue("\"@query\": ?filter=active&sort=name&page=1&limit=10" in signatureBase)
        assertTrue("\"@authority\": api.example.com" in signatureBase)
        assertTrue("\"@scheme\": https" in signatureBase)
        assertTrue("\"@target-uri\": https://api.example.com:8443/users/123?filter=active&sort=name&page=1&limit=10" in signatureBase)
        assertTrue("\"@request-target\": /users/123?filter=active&sort=name&page=1&limit=10" in signatureBase)

        assertTrue("\"@query-param;name=\"filter\"\": active" in signatureBase)
        assertTrue("\"@query-param;name=\"sort\"\": name" in signatureBase)
        assertTrue("\"@query-param;name=\"page\"\": 1" in signatureBase)
        assertTrue("\"@query-param;name=\"limit\"\": 10" in signatureBase)

        assertTrue("\"host\": api.example.com" in signatureBase)
        assertTrue("\"date\": Wed, 01 Nov 2023 12:30:45 GMT" in signatureBase)
        assertTrue("\"authorization\": Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." in signatureBase)
        assertTrue("\"content-type\": application/json" in signatureBase)
        assertTrue("\"content-length\": 57" in signatureBase)
        assertTrue("\"content-digest\": sha-256=:base64encodeddigest:" in signatureBase)
        assertTrue("\"cache-control\": no-cache, max-age=0" in signatureBase)
        assertTrue("\"accept\": application/json" in signatureBase)
        assertTrue("\"x-request-id\": f058ebd6-02f7-4d3f-942e-904344e8cde5" in signatureBase)

        assertTrue("\"@signature-params\":" in signatureBase)

        val paramsLine = signatureBase.lines().last()

        allComponents.forEach { component ->
            when (component) {
                is QueryParam -> {
                    val paramName = component.params["name"]
                    assertTrue(
                        paramsLine.contains("\"${component.name}") && paramsLine.contains("\"$paramName\""),
                        "Signature params should include query param ${component.name} with name $paramName"
                    )
                }

                else -> assertTrue(
                    paramsLine.contains("\"${component.name}\""),
                    "Signature params should include ${component.name}"
                )
            }
        }

        assertTrue(";created=1672574400" in paramsLine)
        assertTrue(";expires=1672574700" in paramsLine)
        assertTrue(";keyid=\"test-key-rsa-2048\"" in paramsLine)
        assertTrue(";alg=\"rsa-pss-sha512\"" in paramsLine)
        assertTrue(";nonce=\"abc123xyz789\"" in paramsLine)
        assertTrue(";tag=\"api-gateway\"" in paramsLine)

        val keyPair = generateRsaKeyPair()

        val signature = RsaPssSha512.sign(signatureBase, keyPair.private)

        assertTrue(signature.startsWith(":"))
        assertTrue(signature.endsWith(":"))

        assertTrue(
            RsaPssSha512.verify(signatureBase, signature, keyPair.public),
            "Signature should verify with the correct public key"
        )

        val differentKeyPair = generateRsaKeyPair()
        assertFalse(
            RsaPssSha512.verify(signatureBase, signature, differentKeyPair.public),
            "Signature should not verify with an incorrect public key"
        )

        val tamperedBase = signatureBase.replace("POST", "GET")
        assertFalse(
            RsaPssSha512.verify(tamperedBase, signature, keyPair.public),
            "Signature should not verify when the signature base is tampered with"
        )
    }

    private fun generateRsaKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048, SecureRandom())
        val javaKeyPair = keyPairGenerator.generateKeyPair()

        return KeyPair(
            public = javaKeyPair.public as RSAPublicKey,
            private = javaKeyPair.private as RSAPrivateKey
        )
    }

    data class KeyPair(val public: RSAPublicKey, val private: RSAPrivateKey)
}
