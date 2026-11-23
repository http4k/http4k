/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt

import org.testcontainers.containers.GenericContainer

val redisUrl: String by lazy {
    val redis = GenericContainer<Nothing>("redis:7-alpine")
    redis.addExposedPort(6379)
    redis.start()
    "redis://${redis.host}:${redis.getMappedPort(6379)}"
}
