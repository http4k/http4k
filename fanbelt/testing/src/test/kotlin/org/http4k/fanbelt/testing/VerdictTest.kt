package org.http4k.fanbelt.testing

import com.natpryce.hamkrest.MatchResult.Match
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict.Accepted
import org.http4k.fanbelt.testing.Verdict.Denied
import org.http4k.fanbelt.testing.Verdict.Hidden
import org.http4k.fanbelt.testing.Verdict.Rejected
import org.http4k.fanbelt.testing.Verdict.Unavailable
import org.junit.jupiter.api.Test

class VerdictTest {
    @Test
    fun `a refusal matches only the refusal named, or an action the page never offered`() {
        val matched = listOf(Accepted, Rejected("no"), Denied, Hidden, Unavailable).filter { refusedAs(Denied)(it) == Match }

        assertThat(matched, equalTo(listOf(Denied, Unavailable)))
    }
}
