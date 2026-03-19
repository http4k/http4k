/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.connect.x402

import org.http4k.connect.WithRunningFake

class RunningFakeX402FacilitatorTest : X402FacilitatorContract, WithRunningFake(::FakeX402Facilitator)
