import okhttp3.OkHttpClient
import org.http4k.client.OkHttp
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Request
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Duration
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

fun main() {
    println(
        OkHttp(client())(
            Request(
                POST,
                "https://localhost:31001/api/v1/web/guest/foo/testFunction/check-image"
            ).body(Body(ByteBuffer.wrap(Request.javaClass.getResourceAsStream("/test.png").readBytes())))
        )
    )
}

private fun client(): OkHttpClient {
    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        }

        override fun getAcceptedIssuers() = arrayOf<X509Certificate>()
    })

    val sslSocketFactory = SSLContext.getInstance("SSL").apply {
        init(null, trustAllCerts, SecureRandom())
    }.socketFactory

    return OkHttpClient.Builder()
        .readTimeout(Duration.ofMinutes(2))
        .callTimeout(Duration.ofMinutes(2))
        .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
        .hostnameVerifier(HostnameVerifier { _, _ -> true }).build()
}
