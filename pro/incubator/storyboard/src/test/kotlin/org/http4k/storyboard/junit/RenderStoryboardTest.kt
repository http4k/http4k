package org.http4k.storyboard.junit

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.storyboard.Storyboard
import org.http4k.storyboard.webDriver
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ExtendWith(JsonApprovalTest::class)
class RenderStoryboardTest {

    companion object {
        private val outputDir: File = Files.createTempDirectory("webdriver-recorder-test").toFile()
    }

    private val handler: HttpHandler = { req ->
        Response.Companion(Status.OK).body("<html><head><title>${req.uri.path}</title></head><body>page</body></html>")
    }

    @JvmField
    @RegisterExtension
    val ext = RenderStoryboard(outputDir, clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))

    @Test
    @Order(1)
    fun `records snapshots`(storyboard: Storyboard) {
        val driver = storyboard.webDriver(handler)
        driver.get("/1")
        driver.capture("Click1", "notes1")
        driver.get("/2")
        driver.capture("Click2", "notes2")
        driver.get("/3")
        driver.capture("Click3", "notes3")
        driver.get("/4")
        driver.capture("Click4", "notes4")
        driver.get("/5")
        driver.capture("Click5", "notes5")
    }

    private val classDir = File(outputDir, this::class.java.name.replace('.', '/'))

    @Test
    @Order(2)
    fun `previous test wrote a JSON file containing the recorded snapshots`(approver: Approver) {
        val expected = File(classDir, "records snapshots.json")

        assertThat(expected.exists(), equalTo(true))

        approver.assertApproved(expected.readText(), ContentType.APPLICATION_JSON)
    }

    @Test
    @Order(3)
    fun `previous test also wrote an HTML report alongside the JSON`() {
        val expected = File(classDir, "records snapshots.html")

        assertThat(expected.exists(), equalTo(true))

        val content = expected.readText()
        assertThat(content, containsSubstring("<title>Storyboard:"))
        assertThat(content, containsSubstring(">Click1</span>"))
        assertThat(content, containsSubstring(">Click5</span>"))
        assertThat(content, containsSubstring("id=\"storyboard-frame\""))
    }

    @Test
    @Order(4)
    fun `the class directory has an index page linking the recorded tests`() {
        val index = File(classDir, "index.html")

        assertThat(index.exists(), equalTo(true))

        val content = index.readText()
        assertThat(content, containsSubstring("<title>Storyboard: RenderStoryboardTest</title>"))
        assertThat(content, containsSubstring("""href="records snapshots.html""""))
    }
}
