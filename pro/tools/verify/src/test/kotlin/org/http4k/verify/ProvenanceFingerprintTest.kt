/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

class ProvenanceFingerprintTest {

    @Test
    fun `extracts fingerprint from provenance JSON`() {
        val provenance = """
        {
          "_type": "https://in-toto.io/Statement/v1",
          "predicate": {
            "buildDefinition": {},
            "runDetails": {},
            "signingKey": {
              "fingerprint": "sha256:abc123def456"
            }
          }
        }
        """.trimIndent()

        assertThat(extractSigningFingerprint(provenance), equalTo(KeyFingerprint.of("sha256:abc123def456")))
    }

    @Test
    fun `throws when no signingKey present`() {
        val provenance = """
        {
          "_type": "https://in-toto.io/Statement/v1",
          "predicate": {
            "buildDefinition": {},
            "runDetails": {}
          }
        }
        """.trimIndent()

        assertThat({ extractSigningFingerprint(provenance) }, throws<IllegalStateException>())
    }

    @Test
    fun `throws for invalid JSON`() {
        assertThat({ extractSigningFingerprint("not json") }, throws<Exception>())
    }

    @Test
    fun `throws when fingerprint field is missing`() {
        val provenance = """
        {
          "_type": "https://in-toto.io/Statement/v1",
          "predicate": {
            "signingKey": {}
          }
        }
        """.trimIndent()

        assertThat({ extractSigningFingerprint(provenance) }, throws<IllegalStateException>())
    }
}
