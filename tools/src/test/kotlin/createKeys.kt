import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object KeyTool {
    private const val algorithm = "RSA"
    private val keyFactory = KeyFactory.getInstance(algorithm)

    fun loadKeyPair(): Pair<RSAPublicKey, RSAPrivateKey> {
        val privateKey = javaClass.getResourceAsStream("/liveKey.priv").use {
            keyFactory.generatePrivate(PKCS8EncodedKeySpec(it.readBytes())) as RSAPrivateKey
        }

        val publicKey = javaClass.getResourceAsStream("/liveKey.pub").use {
            keyFactory.generatePublic(X509EncodedKeySpec(it.readBytes())) as RSAPublicKey
        }

        println(publicKey.asJwk())

        return publicKey to privateKey
    }
}

fun RSAPublicKey.asJwk() = Jwk(
    e = Base64.getUrlEncoder().encodeToString(publicExponent.toByteArray()),
    kid = "some-kid",
    n = Base64.getUrlEncoder().encodeToString(modulus.toByteArray()),
)

data class Jwk(
    val e: String,
    val kid: String,
    val n: String
) {
    val alg = "RSA256"
    val kty = "RSA"
    val use = "sig"
}
