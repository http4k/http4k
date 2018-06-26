package org.http4k.chaos

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.chaos.ChaosBehaviour.Companion.BlockThread
import org.http4k.chaos.ChaosBehaviour.Companion.Latency
import org.http4k.chaos.ChaosBehaviour.Companion.NoBody
import org.http4k.chaos.ChaosPolicy.Companion.Always
import org.http4k.chaos.ChaosPolicy.Companion.PercentageBased
import org.http4k.chaos.ChaosStage.Companion.Wait
import org.http4k.chaos.Triggers.TimePast
import org.http4k.core.HttpTransaction
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.Test
import java.time.Duration.ZERO
import java.time.Duration.ofMillis
import java.time.Duration.ofSeconds

class ChaosPolicyTest {

    @Test
    fun `Always applies by default`() {
        val tx = HttpTransaction(Request(GET, ""), Response(OK).body("hello"), ZERO)
        assertThat(Always.appliesTo(tx), equalTo(true))
        assertThat(Always.inject(NoBody())(tx), equalTo(Response(OK)))
    }

    val blockThread = Wait.until(TimePast(ofSeconds(100))).then(PercentageBased(100).inject(BlockThread()))
    val goSlow = Wait.until(TimePast(ofSeconds(100))).then(Always.inject(Latency(ofMillis(1))))
    val a = ChaosStage.Repeat { blockThread.then(goSlow) }.until { _ -> true }
}