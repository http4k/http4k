package org.http4k.security.signature

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.UNAUTHORIZED
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ServerFilters
import org.http4k.filter.SignHttpRequest
import org.http4k.filter.VerifyHttpSignature
import org.http4k.security.signature.SignatureComponent.Method
import org.http4k.security.signature.SignatureComponent.Path
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

class HttpSignatureFiltersTest {

    private val keyPair =
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048, SecureRandom()) }.generateKeyPair()

    private val keyId = "test-key-id"

    @Test
    fun `client filter adds signature headers to request`() {
        val signingFilter = ClientFilters.SignHttpRequest(
            HttpMessageSigner(listOf(Method, Path), keyId, keyPair.private, RsaPssSha512)
        )

        val echoHandler: HttpHandler = { req -> Response(OK).body(req.toString()) }

        val signedRequest = captureRequestFromFilter(signingFilter, echoHandler, Request(GET, "/hello"))

        assertNotNull(signedRequest.header("Signature-Input"))
        assertNotNull(signedRequest.header("Signature"))

        val signatureInput = signedRequest.header("Signature-Input") ?: ""
        assert(signatureInput.contains("@method")) { "Signature-Input should include @method" }
        assert(signatureInput.contains("@path")) { "Signature-Input should include @path" }
    }

    @Test
    fun `server filter rejects requests with missing required components`() {
        val keyStore = ConcurrentHashMap<String, java.security.PublicKey>()
        keyStore[keyId] = keyPair.public

        val signingFilter = ClientFilters.SignHttpRequest(
            HttpMessageSigner(listOf(Method), keyId, keyPair.private, RsaPssSha512)
        )

        val signedRequest = captureRequestFromFilter(signingFilter, { Response(OK) }, Request(GET, "/hello"))

        val verifyingFilter = ServerFilters.VerifyHttpSignature(
            HttpMessageSignatureVerifier(
                SignatureComponentFactory.HttpRequest(),
                keyStore::get,
                RsaPssSha512,
                listOf(Method, Path)
            )
        )

        val response = verifyingFilter.then { _: Request -> Response(OK).body("Success") }(signedRequest)

        assertEquals(UNAUTHORIZED, response.status)
    }

    @Test
    fun `server filter accepts requests with all required components`() {
        val keyStore = ConcurrentHashMap<String, java.security.PublicKey>()
        keyStore[keyId] = keyPair.public

        val components = listOf(Method, Path)

        val signingFilter = ClientFilters.SignHttpRequest(
            HttpMessageSigner(components, keyId, keyPair.private, RsaPssSha512)
        )

        val signedRequest = captureRequestFromFilter(signingFilter, { Response(OK) }, Request(GET, "/hello"))

        val verifyingFilter = ServerFilters.VerifyHttpSignature(
            HttpMessageSignatureVerifier(
                SignatureComponentFactory.HttpRequest(),
                keyStore::get,
                RsaPssSha512,
                listOf(Method, Path)
            )
        )

        val response = verifyingFilter.then { _: Request -> Response(OK).body("Success") }(signedRequest)

        assertEquals(OK, response.status)
    }

    private fun captureRequestFromFilter(filter: Filter, handler: HttpHandler, request: Request): Request {
        var capturedRequest: Request? = null

        val capturingHandler: HttpHandler = { req ->
            capturedRequest = req
            handler(req)
        }

        filter.then(capturingHandler)(request)

        return capturedRequest ?: throw IllegalStateException("Request was not captured")
    }
}
