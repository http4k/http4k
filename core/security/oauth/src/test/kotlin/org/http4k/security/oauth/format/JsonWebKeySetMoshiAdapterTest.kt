package org.http4k.security.oauth.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.security.oauth.metadata.JsonWebKey
import org.http4k.security.oauth.metadata.JsonWebKeySet
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(JsonApprovalTest::class)
class JsonWebKeySetMoshiAdapterTest {
    private val marshaller = OAuthMoshi

    @Test
    fun `deserialize with minimal fields`() {
        val json = """{
            "keys": [{
                "kty": "RSA"
            }]
        }"""

        assertThat(
            marshaller.asA(json), equalTo(
                JsonWebKeySet(
                    keys = listOf(
                        JsonWebKey(
                            kty = "RSA"
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `deserialize with full fields`() {
        val json = """{
            "keys": [{
                "kty": "RSA",
                "use": "sig",
                "kid": "2011-04-29",
                "alg": "RS256",
                "n": "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw",
                "e": "AQAB",
                "x5c": ["cert1", "cert2"],
                "x5t": "thumb1",
                "x5t#S256": "thumb256"
            }]
        }"""

        assertThat(
            marshaller.asA(json), equalTo(
                JsonWebKeySet(
                    keys = listOf(
                        JsonWebKey(
                            kty = "RSA",
                            use = "sig",
                            kid = "2011-04-29",
                            alg = "RS256",
                            n = "0vx7agoebGcQSuuPiLJXZptN9nndrQmbXEps2aiAFbWhM78LhWx4cbbfAAtVT86zwu1RK7aPFFxuhDR1L6tSoc_BJECPebWKRXjBZCiFV4n3oknjhMstn64tZ_2W-5JsGY4Hc5n9yBXArwl93lqt7_RN5w6Cf0h4QyQ5v-65YGjQR0_FDW2QvzqY368QQMicAtaSqzs8KJZgnYb9c7d0zgdAZHzu6qMQvRL5hajrn1n91CbOpbISD08qNLyrdkt-bFTWhAI4vMQFh6WeZu0fM4lFd2NcRwr3XPksINHaQ-G_xBniIqbw0Ls1jF44-csFCur-kEgU8awapJzKnqDKgw",
                            e = "AQAB",
                            x5c = listOf("cert1", "cert2"),
                            x5t = "thumb1",
                            `x5t#S256` = "thumb256"
                        )
                    )
                )
            )
        )
    }

    @Test
    fun `deserialize multiple keys`() {
        val json = """{
            "keys": [
                {"kty": "RSA", "kid": "key1"},
                {"kty": "EC", "kid": "key2"}
            ]
        }"""

        assertThat(
            marshaller.asA(json), equalTo(
                JsonWebKeySet(
                    keys = listOf(
                        JsonWebKey(kty = "RSA", kid = "key1"),
                        JsonWebKey(kty = "EC", kid = "key2")
                    )
                )
            )
        )
    }

    @Test
    fun `serialize full key`(approver: Approver) {
        val data = JsonWebKeySet(
            keys = listOf(
                JsonWebKey(
                    kty = "RSA",
                    use = "sig",
                    kid = "2011-04-29",
                    alg = "RS256",
                    n = "n-value",
                    e = "AQAB",
                    x5c = listOf("cert1", "cert2"),
                    x5t = "thumb1",
                    `x5t#S256` = "thumb256"
                )
            )
        )

        approver.assertApproved(marshaller.asFormatString(data), ContentType.APPLICATION_JSON)
    }

    @Test
    fun `serialize minimal key`(approver: Approver) {
        val data = JsonWebKeySet(
            keys = listOf(
                JsonWebKey(
                    kty = "RSA"
                )
            )
        )

        approver.assertApproved(marshaller.asFormatString(data), ContentType.APPLICATION_JSON)
    }
}
