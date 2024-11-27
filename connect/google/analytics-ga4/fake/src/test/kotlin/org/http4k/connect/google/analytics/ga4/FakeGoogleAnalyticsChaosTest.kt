package org.http4k.connect.google.analytics.ga4

import org.http4k.connect.FakeSystemContract
import org.http4k.core.Method
import org.http4k.core.Request

class FakeGoogleAnalyticsChaosTest : FakeSystemContract(FakeGoogleAnalytics()) {
    override val anyValid = Request(Method.POST, "/mp/collect")
}
