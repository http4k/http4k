package org.http4k.storyboard

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import java.nio.file.Files

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ExtendWith(JsonApprovalTest::class)
class StoryboardExtensionTest {

    companion object {
        private val outputDir: File = Files.createTempDirectory("webdriver-recorder-test").toFile()
        private val handler: HttpHandler = { req ->
            Response(OK).body("<html><head><title>${req.uri.path}</title></head><body>page</body></html>")
        }
    }

    @JvmField
    @RegisterExtension
    val ext = Storyboard(handler, outputDir)

    @Test
    @Order(1)
    fun `records snapshots`(driver: RecordingWebDriver) {
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

    @Test
    @Order(2)
    fun `previous test wrote a JSON file containing the recorded snapshots`(approver: Approver) {
        val expected = File(
            outputDir,
            "org.http4k.storyboard.StoryboardExtensionTest.records snapshots.json"
        )

        assertThat(expected.exists(), equalTo(true))

        approver.assertApproved(expected.readText(), ContentType.APPLICATION_JSON)
    }

    @Test
    @Order(3)
    fun `previous test also wrote an HTML report alongside the JSON`() {
        val expected = File(
            outputDir,
            "org.http4k.storyboard.StoryboardExtensionTest.records snapshots.html"
        )

        assertThat(expected.exists(), equalTo(true))

        val content = expected.readText()
        assertThat(content, containsSubstring("<title>Storyboard:"))
        assertThat(content, containsSubstring(">Click1</span>"))
        assertThat(content, containsSubstring(">Click5</span>"))
        assertThat(content, containsSubstring("id=\"storyboard-frame\""))
    }

    @Test
    @Order(4)
    fun `each test method gets an independent driver instance`(driver: RecordingWebDriver) {
        assertThat(driver.frames(), equalTo(emptyList()))
        driver.get("http://localhost/fresh")
        driver.capture("only one")
        assertThat(driver.frames().size, equalTo(1))
    }
}
