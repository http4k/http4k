/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.bus

import org.http4k.events.Event
import org.http4k.fanbelt.events.FanbeltFailure

data class SubscriberFailed(override val cause: Throwable) : FanbeltFailure

data class BackplaneSendFailed(val topic: Topic, override val cause: Throwable) : FanbeltFailure

data class BackplaneConnectFailed(override val cause: Throwable) : FanbeltFailure

data class BackplaneDisconnected(override val cause: Throwable) : FanbeltFailure

data object BackplaneReconnected : Event
