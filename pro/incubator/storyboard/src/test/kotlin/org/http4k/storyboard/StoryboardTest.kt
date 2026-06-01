package org.http4k.storyboard

import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class StoryboardTest {

    @JvmField
    @RegisterExtension
    val ext = Storyboard({ req ->
        Response(OK).body("<html><head><title>${req.uri.path}</title></head><body>page</body></html>")
    })

    @Test
    fun `records frames`(driver: RecordingWebDriver) {
        driver.get("http://localhost/1")
        driver.capture("Click1", "notes1")
        driver.get("http://localhost/2")
        driver.capture("Click2", "notes2")
        driver.get("http://localhost/3")
        driver.capture("Click3", "notes3")
        driver.get("http://localhost/4")
        driver.capture("Click4", "notes4")
        driver.get("http://localhost/5")
        driver.capture("Click5", "notes5")
    }
}
