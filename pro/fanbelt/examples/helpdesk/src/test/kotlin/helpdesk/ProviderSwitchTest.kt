package helpdesk

import helpdesk.domain.CustomerEmail
import helpdesk.domain.Subject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ProviderSwitchTest {
    private val driver = UiHelpdeskDriver()

    @Test
    fun `the provider switch flips the fake provider and shows where it now stands`() {
        val sam = driver.agent("sam")
        val billing = driver.customerOpens(Subject.of("Billing question"), from = CustomerEmail.of("bo@example.com"))
        assertEquals("Email provider: up", sam.seesProviderOn(billing))

        sam.switchesProvider()

        assertEquals("Email provider: DOWN", sam.seesProviderOn(billing))
        assertEquals(false, driver.providerIsUp())
    }
}
