package org.http4k.chaos

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.chaos.ChaosBehaviour.Companion.BlockThread
import org.http4k.chaos.ChaosBehaviour.Companion.Latency
import org.http4k.chaos.ChaosPolicy.Companion.Always
import org.http4k.chaos.ChaosPolicy.Companion.PercentageBased
import org.http4k.chaos.ChaosStage.Companion.Wait
import org.http4k.chaos.Triggers.TimePast
import org.http4k.core.Request
import org.junit.Test
import java.time.Duration

class ChaosPolicyTest {

    @Test
    fun `asd`() {
        ChaosPolicy.Always(injectRequest = true)
        assertThat(false, equalTo(false))
    }

    val blockThread = Wait.until(TimePast(Duration.ofSeconds(100))).then(PercentageBased(100).inject(BlockThread()))
    val goSlow = Wait.until(TimePast(Duration.ofSeconds(100))).then(Always().inject(Latency(Duration.ofMillis(1))))
    val a = ChaosStage.Repeat { blockThread.then(goSlow) }.until { _: Request -> true }
}