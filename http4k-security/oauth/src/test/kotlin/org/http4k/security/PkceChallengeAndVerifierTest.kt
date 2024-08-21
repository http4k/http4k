package org.http4k.security

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import kotlin.random.Random

class PkceChallengeAndVerifierTest {
    @Test
    fun `PKCE challenge and verifier`() {
        val (challenge, verifier) = PkceChallengeAndVerifier.create(Random(seed = 123))
        assertThat(challenge, equalTo("JyVCyfhalScAolAKqL-6lHz0WN_f0y6MppuWhGpvmUo"))
        assertThat(verifier, equalTo("C.in5gk-88wM1WudMeo78XKF4x2I_vlxVhTk8S82cyafJQ~nBT7MgSOSV51DabN6LrJiP~1Gy9ORyB8h_9rJkieXi_YkSvnTRO7UluTEcjYf~kmcfdA8HhVryrlxc~_3"))
    }
}

