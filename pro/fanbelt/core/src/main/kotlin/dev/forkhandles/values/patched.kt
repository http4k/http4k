/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package dev.forkhandles.values

import java.util.UUID

fun <DOMAIN : Value<UUID>> UUIDValueFactory<DOMAIN>.random(random: java.util.Random) = of(random.nextLong(), random.nextLong())
