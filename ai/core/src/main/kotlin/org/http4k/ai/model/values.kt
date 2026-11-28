package org.http4k.ai.model

import dev.forkhandles.values.Validation
import dev.forkhandles.values.and
import dev.forkhandles.values.toValidator

internal val _0_to_1 = 0.toDouble().min.and(1.toDouble().max).toValidator()
internal val Double.min: Validation<Double> get() = { it >= this }
internal val Double.max: Validation<Double> get() = { it <= this }
