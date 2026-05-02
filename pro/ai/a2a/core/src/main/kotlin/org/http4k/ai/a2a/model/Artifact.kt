/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.model

data class Artifact(
    val artifactId: ArtifactId,
    val parts: List<Part>,
    val name: String? = null,
    val description: String? = null,
    val metadata: Map<String, Any>? = null,
    val extensions: List<String>? = null
)
