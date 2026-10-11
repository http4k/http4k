/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.postbox.processing

import java.time.Duration
import kotlin.math.pow

typealias BackoffStrategy = (failures: Int, random: RandomSource) -> Duration

fun defaultBackoffStrategy(failures: Int, random: RandomSource): Duration = Duration.ofMillis(
    (2.0.pow(failures.toDouble()) * Duration.ofSeconds(5).toMillis() + random(10) * 1000).toLong()
)
