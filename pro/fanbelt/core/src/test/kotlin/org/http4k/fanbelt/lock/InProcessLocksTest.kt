/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.lock

class InProcessLocksTest : LocksContract() {
    override val locks = InProcessLocks()
    override val elsewhere = locks
}
