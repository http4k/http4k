package guide.reference.jwt

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey

class RsaProvider {

    private val keyPair = KeyPairGenerator.getInstance("RSA")
        .apply { initialize(2048) }
        .generateKeyPair()

    val public get() = keyPair.public as RSAPublicKey

    fun newJwt(subject: String): String {
        val header = JWSHeader.Builder(JWSAlgorithm.RS256).build()
        val claims = JWTClaimsSet.Builder()
            .subject(subject)
            .build()

        return SignedJWT(header, claims)
            .apply { sign(RSASSASigner(keyPair.private)) }
            .serialize()
    }
}
