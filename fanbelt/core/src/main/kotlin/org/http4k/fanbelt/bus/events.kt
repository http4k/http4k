package org.http4k.fanbelt.bus

import org.http4k.events.Event
import org.http4k.fanbelt.events.FanbeltFailure

data class SubscriberFailed(override val cause: Throwable) : FanbeltFailure

data class BackplaneSendFailed(val topics: Set<Topic>, override val cause: Throwable) : FanbeltFailure

data class BackplaneConnectFailed(override val cause: Throwable) : FanbeltFailure

data class BackplaneDisconnected(override val cause: Throwable) : FanbeltFailure

data object BackplaneReconnected : Event

data class BackplaneSubscribeDelayed(val interest: Interest) : Event
