/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.security.passkeys

import org.http4k.connect.model.Base64UriBlob
import java.security.SecureRandom

fun Base64UriBlob.Companion.randomChallenge(random: SecureRandom = SecureRandom()): Base64UriBlob =
    Base64UriBlob.encode(ByteArray(32).also(random::nextBytes))

fun Base64UriBlob.Companion.randomHandle(random: SecureRandom = SecureRandom()): Base64UriBlob =
    Base64UriBlob.encode(ByteArray(16).also(random::nextBytes))
