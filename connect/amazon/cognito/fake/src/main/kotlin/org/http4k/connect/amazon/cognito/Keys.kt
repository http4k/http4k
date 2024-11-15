package org.http4k.connect.amazon.cognito

import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

object Keys {
    private val keyFactory = KeyFactory.getInstance("RSA")

    val live = loadKeyPair("liveKey")
    val expired = loadKeyPair("expiredKey")

    private fun loadKeyPair(baseName: String): Pair<RSAPublicKey, RSAPrivateKey> {
        val privateKey = javaClass.getResourceAsStream("/$baseName.priv")!!.use {
            keyFactory.generatePrivate(PKCS8EncodedKeySpec(it.readBytes())) as RSAPrivateKey
        }

        val publicKey = javaClass.getResourceAsStream("/$baseName.pub")!!.use {
            keyFactory.generatePublic(X509EncodedKeySpec(it.readBytes())) as RSAPublicKey
        }

        return publicKey to privateKey
    }

}
