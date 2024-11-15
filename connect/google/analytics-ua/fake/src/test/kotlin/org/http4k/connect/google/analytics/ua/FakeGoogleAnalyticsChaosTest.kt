package org.http4k.connect.google.analytics.ua

import org.http4k.connect.FakeSystemContract
import org.http4k.connect.google.ua.FakeGoogleAnalytics
import org.http4k.core.Method
import org.http4k.core.Request

class FakeGoogleAnalyticsChaosTest : FakeSystemContract(FakeGoogleAnalytics()) {
    override val anyValid = Request(Method.POST, "/collect")
}
