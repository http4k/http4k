package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.Test
import org.openqa.selenium.By
import java.io.File
import java.net.URL

class Http4kWebDriverTest {
    private val driver = Http4kWebDriver {
        req ->
        val body = File("src/test/resources/test.html").readText()
        Response(OK).body(body
            .replace("THEURL", req.uri.path)
            .replace("THETIME", System.currentTimeMillis().toString())
        )
    }

    @Test
    fun `page details`() {
        driver.get("/bob")
        assertThat(driver.currentUrl, equalTo("/bob"))
        assertThat(driver.title, equalTo("Page title"))
        assertThat(driver.findElement(By.id("firstId"))!!.text, equalTo("the first text"))
    }

    @Test
    fun `navigation`() {
        driver.navigate().to("/rita")
        assertThat(driver.findElement(By.tagName("h1"))!!.text, equalTo("/rita"))

        driver.navigate().to(URL("http://localhost/bob"))
        assertThat(driver.findElement(By.tagName("h1"))!!.text, equalTo("/bob"))
        driver.get("/bill")
        assertThat(driver.findElement(By.tagName("h1"))!!.text, equalTo("/bill"))
        driver.navigate().back()
        assertThat(driver.findElement(By.tagName("h1"))!!.text, equalTo("/bob"))
        driver.navigate().forward()
        assertThat(driver.findElement(By.tagName("h1"))!!.text, equalTo("/bill"))
        val preRefreshTime = driver.findElement(By.tagName("h2"))!!.text
        driver.navigate().refresh()
        assertThat(driver.findElement(By.tagName("h1"))!!.text, equalTo("/bill"))
        assertThat(driver.findElement(By.tagName("h2"))!!.text, !equalTo(preRefreshTime))
    }

    @Test
    fun `single window lifecycle`() {
        assertThat(driver.windowHandles.size, equalTo(0))
        driver.get("/bill")
        assertThat(driver.windowHandles.size, equalTo(1))
        assertThat(driver.windowHandle, present())
        driver.quit()
        assertThat(driver.windowHandles.size, equalTo(0))
        driver.get("/bill")
        assertThat(driver.windowHandles.size, equalTo(1))
        assertThat(driver.windowHandle, present())
        driver.close()
        assertThat(driver.windowHandles.size, equalTo(0))
    }

    @Test
    fun `active element`() {
        driver.get("/bill")
        assertThat(driver.switchTo().activeElement().tagName, equalTo("div"))
    }

    @Test
    fun `unsupported features`() {
        driver.get("/bill")

        val windowHandle = driver.windowHandle

        isNotImplemented {driver.manage()}
        isNotImplemented {driver.switchTo().alert()}
        isNotImplemented {driver.switchTo().frame(0)}
        isNotImplemented {driver.switchTo().frame("bob")}
        isNotImplemented {driver.switchTo().frame(windowHandle)}
        isNotImplemented {driver.switchTo().parentFrame()}
    }
}