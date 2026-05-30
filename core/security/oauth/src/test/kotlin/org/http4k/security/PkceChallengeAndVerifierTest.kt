package org.http4k.security

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.util.Random

class PkceChallengeAndVerifierTest {
    @Test
    fun `PKCE challenge and verifier`() {
        val (challenge, verifier) = PkceChallengeAndVerifier.create(Random(0))
        assertThat(challenge, equalTo("8h0AMgROjTvGV6kx3HcJXXhzysDul-dTOPxjQHuU3Y4"))
        assertThat(verifier, equalTo("AcTFRjjn5sjjjQj4cb1grS-1gEtck7XLX~4mPvu5I3XCQtcu083Dyqb6_~t3vcfMS5Ts6SmEvjmeFAT0Z..T.GT3zZPck2gHVMtVCbj1D9JP2HQdE9NrkrMhhOaN0g_y"))
    }
}
