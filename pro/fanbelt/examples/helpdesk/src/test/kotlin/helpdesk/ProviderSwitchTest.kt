package helpdesk

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import helpdesk.domain.CustomerEmail
import helpdesk.domain.Subject
import org.junit.jupiter.api.Test

class ProviderSwitchTest {
    private val driver = UiHelpdeskDriver()

    @Test
    fun `the provider switch flips the fake provider and shows where it now stands`() {
        val sam = driver.agent("sam")
        val billing = driver.customerOpens(Subject.of("Billing question"), from = CustomerEmail.of("bo@example.com"))
        assertThat(sam.seesProviderOn(billing), equalTo("Email provider: up"))

        sam.switchesProvider()

        assertThat(sam.seesProviderOn(billing), equalTo("Email provider: DOWN"))
        assertThat(driver.providerIsUp(), equalTo(false))
    }
}
