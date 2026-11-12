/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.infrastructure

import org.http4k.config.EnvironmentKey
import org.http4k.fanbelt.tenancy.RedrawLimit
import org.http4k.lens.duration
import org.http4k.lens.of
import org.http4k.lens.value
import java.time.Duration.ofMinutes
import java.time.Duration.ofSeconds

/**
 * Fanbelt's own settings: how a node behaves, as opposed to which technology it runs on.
 * Each is read from the Environment given to AppInfra (empty by default, so every one
 * takes its default), under an environment variable of the same name.
 */
object FanbeltSettings {
    val FANBELT_REDRAW_LIMIT by EnvironmentKey.value(RedrawLimit).of().defaulted(RedrawLimit.of(8))
    val FANBELT_PRESENCE_HEARTBEAT by EnvironmentKey.duration().of().defaulted(ofSeconds(10))
    val FANBELT_PRESENCE_TTL by EnvironmentKey.duration().of().defaulted(ofSeconds(30))
    val FANBELT_STREAM_KEEPALIVE by EnvironmentKey.duration().of().defaulted(ofSeconds(2))
    val FANBELT_BACKPLANE_HEAL by EnvironmentKey.duration().of().defaulted(ofMinutes(5))
}
