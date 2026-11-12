/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.presence

import org.http4k.events.Event
import org.http4k.fanbelt.events.FanbeltFailure

data class PresenceRenewalFailed(override val cause: Throwable) : FanbeltFailure

data object PresenceRecovered : Event
