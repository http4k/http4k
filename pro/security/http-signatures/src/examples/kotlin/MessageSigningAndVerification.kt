package example

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ServerFilters
import org.http4k.filter.SignHttpRequest
import org.http4k.filter.VerifyHttpSignature
import org.http4k.security.signature.HttpMessageSignatureVerifier
import org.http4k.security.signature.HttpMessageSigner
import org.http4k.security.signature.RsaPssSha512
import org.http4k.security.signature.SignatureComponent.Authority
import org.http4k.security.signature.SignatureComponent.Header
import org.http4k.security.signature.SignatureComponent.Method
import org.http4k.security.signature.SignatureComponent.Path
import org.http4k.security.signature.SignatureComponentFactory.Companion.HttpRequest
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.security.KeyPairGenerator
import java.security.SecureRandom

/**
 * A complete example showing HTTP signatures in action, with:
 * - Key generation
 * - A client that signs requests
 * - A server that verifies signatures
 * - Custom polymorphic component example
 */
fun main() {
    val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
    keyPairGenerator.initialize(2048, SecureRandom())
    val keyPair = keyPairGenerator.generateKeyPair()
    val keyId = "demo-key-1"

    val CUSTOM_COMPONENTS = listOf(Method, Path, Authority, Header("content-type"))

    val server = ServerFilters.VerifyHttpSignature(
        HttpMessageSignatureVerifier(
            componentsFactory = HttpRequest(),
            { id -> keyPair.public },
            RsaPssSha512,
            CUSTOM_COMPONENTS
        )
    ).then { request ->
        Response(OK)
            .header("Content-Type", "application/json")
            .body("""{"message": "Signature verified successfully", "path": "${request.uri.path}"}""")
    }

    val port = 8000
    val runningServer = server.asServer(SunHttp(port)).start()
    println("Server started on port $port")

    try {
        val client = ClientFilters.SetBaseUriFrom(Uri.of("http://localhost:$port"))
            .then(
                ClientFilters.SignHttpRequest(
                    HttpMessageSigner(
                        components = CUSTOM_COMPONENTS,
                        keyId = keyId,
                        privateKey = keyPair.private,
                        algorithm = RsaPssSha512,
                        tag = "example-signature"
                    )
                )
            )
            .then(JavaHttpClient())

        val request = Request(GET, "/hello")
            .header("Content-Type", "application/json")

        println("Sending signed request to /hello")
        val response = client(request)

        println("Response status: ${response.status}")
        println("Response body: ${response.bodyString()}")

        val request2 = Request(GET, "/data")
            .header("Content-Type", "application/json")

        println("\nSending signed request to /data")
        val response2 = client(request2)

        println("Response status: ${response2.status}")
        println("Response body: ${response2.bodyString()}")

        // Try a request with missing header (will fail verification)
        val request3 = Request(GET, "/broken")

        // Missing Content-Type header which is required by StandardComponents.STANDARD
        val brokenClient = ClientFilters.SetBaseUriFrom(Uri.of("http://localhost:$port"))
            .then(
                ClientFilters.SignHttpRequest(
                    HttpMessageSigner(
                        components = CUSTOM_COMPONENTS.minus(Header("content-type")),
                        keyId = keyId,
                        privateKey = keyPair.private,
                        algorithm = RsaPssSha512,
                        tag = "example-signature"
                    )
                )
            )
            .then(JavaHttpClient())

        println("\nSending request without Content-Type header")
        val response3 = brokenClient(request3)

        println("Response status: ${response3.status}")
        println("Response body: ${response3.bodyString()}")

    } finally {
        runningServer.stop()
        println("Server stopped")
    }
}
