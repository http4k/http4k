/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.http4k.verify.ArtifactType.jar
import org.http4k.verify.ArtifactType.license
import org.http4k.verify.ArtifactType.provenance
import org.http4k.verify.ArtifactType.sbom
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(JsonApprovalTest::class)
class VerificationReportTest {

    private val fixedTimestamp = Instant.parse("2026-04-05T14:30:00Z")
    private val testFingerprint = KeyFingerprint.of("sha256:test123")
    private val testPluginInfo = PluginInfo(version = "1.0.0", jar_sha256 = "sha256:pluginhash")

    @Test
    fun `generates report with verified module`(approver: Approver) {
        val module = ModuleVerification(
            group = "org.http4k",
            module = "http4k-core",
            version = "5.0.0",
            jarSha256 = "abc123def456",
            checks = mapOf(
                jar to VerificationResult("http4k-core-5.0.0.jar", true, "Verified OK"),
                sbom to VerificationResult("http4k-core-5.0.0-cyclonedx.json", true, "Verified OK"),
                provenance to null,
                license to null
            ),
            exportedFiles = mapOf(
                "jar.bundle" to "org.http4k/http4k-core/5.0.0/http4k-core-5.0.0-jar-sigstore.json",
                "sbom.file" to "org.http4k/http4k-core/5.0.0/http4k-core-5.0.0-cyclonedx.json",
                "sbom.bundle" to "org.http4k/http4k-core/5.0.0/http4k-core-5.0.0-cyclonedx-sigstore.json"
            ),
            signingKeyFingerprint = testFingerprint
        )

        approver.assertApproved(
            VerificationReport.generate(listOf(module), testPluginInfo, fixedTimestamp),
            APPLICATION_JSON
        )
    }

    @Test
    fun `generates report with failed verification`(approver: Approver) {
        val module = ModuleVerification(
            group = "org.http4k",
            module = "http4k-core",
            version = "5.0.0",
            jarSha256 = "abc123",
            checks = mapOf(
                jar to VerificationResult("http4k-core-5.0.0.jar", false, "Signature verification failed"),
                sbom to null,
                provenance to null,
                license to null
            ),
            signingKeyFingerprint = testFingerprint
        )

        approver.assertApproved(
            VerificationReport.generate(listOf(module), testPluginInfo, fixedTimestamp),
            APPLICATION_JSON
        )
    }

    @Test
    fun `generates report with single module`(approver: Approver) {
        val module = ModuleVerification(
            group = "org.http4k",
            module = "http4k-core",
            version = "1.0.0",
            jarSha256 = "abc",
            signingKeyFingerprint = testFingerprint
        )

        approver.assertApproved(
            VerificationReport.generate(listOf(module), testPluginInfo, fixedTimestamp),
            APPLICATION_JSON
        )
    }

    @Test
    fun `generates report with multiple modules`(approver: Approver) {
        val module1 = ModuleVerification("org.http4k", "http4k-core", "5.0.0", "aaa", signingKeyFingerprint = testFingerprint)
        val module2 = ModuleVerification("org.http4k", "http4k-client-okhttp", "5.0.0", "bbb", signingKeyFingerprint = testFingerprint)

        approver.assertApproved(
            VerificationReport.generate(listOf(module1, module2), testPluginInfo, fixedTimestamp),
            APPLICATION_JSON
        )
    }
}
