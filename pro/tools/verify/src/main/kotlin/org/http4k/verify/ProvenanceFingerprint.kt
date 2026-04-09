/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.http4k.format.Moshi

private data class SigningKeyInfo(val fingerprint: String? = null)
private data class ProvenancePredicate(val signingKey: SigningKeyInfo? = null)
private data class ProvenanceStatement(val predicate: ProvenancePredicate? = null)

fun extractSigningFingerprint(provenanceJson: String): KeyFingerprint {
    val fingerprint = Moshi.asA<ProvenanceStatement>(provenanceJson).predicate?.signingKey?.fingerprint
        ?: error("No signingKey.fingerprint found in provenance")
    return KeyFingerprint.of(fingerprint)
}
