import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import okhttp3.OkHttpClient
import org.http4k.client.OkHttp
import org.http4k.client.testImageBytes
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.time.Duration
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object CallTestAction {
    @JvmStatic
    fun main(args: Array<String>) {
        val app = DebuggingFilters.PrintResponse().then(OkHttp(client()))
//    app(Request(POST, "https://localhost:31001/api/v1/web/guest/foo/testFunction/echo").body("helloworld"))
//        app(Request(POST, "https://localhost:31001/api/v1/web/guest/foo/testFunction/check-image").body(Body(ByteBuffer.wrap(testImageBytes()))))
        assertThat(app(Request(Method.GET, "https://localhost:31001/api/v1/web/guest/foo/testFunction/image")).body.payload.array().size, equalTo(
            testImageBytes().size))
    }
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
