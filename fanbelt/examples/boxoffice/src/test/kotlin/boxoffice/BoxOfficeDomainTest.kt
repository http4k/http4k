package boxoffice

import boxoffice.domain.SeatRef
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.fanbelt.testing.Verdict
import org.junit.jupiter.api.Test

class BoxOfficeDomainTest : BoxOfficeContract() {
    override val driver = DomainBoxOfficeDriver()

    @Test
    fun `releasing a seat you never reserved is told you have no reservation of it`() {
        val ann = driver.fan("ann")

        assertThat(ann.releases(SHOW, SeatRef.of("F12")), equalTo(Verdict.Rejected("You have no reservation of F12")))
    }
}
