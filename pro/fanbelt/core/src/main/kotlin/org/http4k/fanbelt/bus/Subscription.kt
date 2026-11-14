/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

/**
 * A subscription or presence lease; close it to end it.
 */
fun interface Subscription : AutoCloseable
