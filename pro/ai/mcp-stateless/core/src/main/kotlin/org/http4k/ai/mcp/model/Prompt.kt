/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.model

import org.http4k.ai.mcp.PromptRequest
import org.http4k.lens.BiDiLens
import org.http4k.lens.BiDiLensSpec
import org.http4k.lens.LensGet
import org.http4k.lens.LensSet
import org.http4k.lens.ParamMeta.StringParam

/**
 * Spec of a Prompt capability.
 */
class Prompt private constructor(
    val name: PromptName,
    val description: String?,
    val args: List<BiDiLens<PromptRequest, *>>,
    val title: String? = null,
    val icons: List<Icon>? = null,
    val cacheScope: CacheScope = CacheScope.public
) : CapabilitySpec {
    constructor(name: PromptName, description: String, vararg args: BiDiLens<PromptRequest, *>, title: String? = null, icons: List<Icon>? = null, cacheScope: CacheScope = CacheScope.public) :
        this(name, description, args.toList(), title, icons, cacheScope)
    constructor(name: String, description: String, vararg args: BiDiLens<PromptRequest, *>, title: String? = null, icons: List<Icon>? = null, cacheScope: CacheScope = CacheScope.public) :
        this(PromptName.of(name), description, args.toList(), title, icons, cacheScope)

    object Arg : BiDiLensSpec<PromptRequest, String>(
        "promptRequest", StringParam,
        LensGet { name, target -> listOfNotNull(target[name]) },
        LensSet { name, values, target -> values.fold(target) { m, v -> m.copy(args = m + (name to v)) } }
    )
}
