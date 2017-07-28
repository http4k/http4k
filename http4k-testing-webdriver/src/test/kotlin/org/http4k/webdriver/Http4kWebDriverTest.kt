package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.junit.Test
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import java.io.File
import java.net.URL

class Http4kWebDriverTest {
    private val driver = Http4kWebDriver {
        req ->
        val body = File("src/test/resources/test.html").readText()
        Response(OK).body(body
            .replace("FORMMETHOD", Method.POST.name)
            .replace("THEMETHOD", req.method.name)
            .replace("THEBODY", req.bodyString())
            .replace("THEURL", req.uri.toString())
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
    fun `POST form`() {
        driver.get("/bob")
        driver.findElement(By.id("button"))!!.submit()
        driver.assertOnPage("/form")
        assertThat(driver.findElement(By.tagName("thebody"))!!.text, equalTo("text1=textValue&text1=&checkbox1=checkbox&checkbox1=&select1=option1&select1=option2"))
        assertThat(driver.findElement(By.tagName("themethod"))!!.text, equalTo("POST"))
    }

    @Test
    fun `GET form`() {
        val driver = Http4kWebDriver {
            req ->
            val body = File("src/test/resources/test.html").readText()
            Response(OK).body(body
                .replace("FORMMETHOD", Method.GET.name)
                .replace("THEMETHOD", req.method.name)
                .replace("THEBODY", req.bodyString())
                .replace("THEURL", req.uri.toString())
                .replace("THETIME", System.currentTimeMillis().toString())
            )
        }

        driver.get("/bob")
        driver.findElement(By.id("button"))!!.submit()
        driver.assertOnPage("/form?text1=textValue&text1=&checkbox1=checkbox&checkbox1=&select1=option1&select1=option2")
        assertThat(driver.findElement(By.tagName("thebody"))!!.text, equalTo(""))
        assertThat(driver.findElement(By.tagName("themethod"))!!.text, equalTo("GET"))
    }

    @Test
    fun `navigation`() {
        driver.navigate().to("http://localhost/rita")
        driver.assertOnPage("http://localhost/rita")

        driver.navigate().to(URL("http://localhost/bob"))
        driver.assertOnPage("http://localhost/bob")
        driver.get("/bill")
        driver.assertOnPage("/bill")
        driver.navigate().back()
        driver.assertOnPage("http://localhost/bob")
        driver.navigate().forward()
        driver.assertOnPage("/bill")
        val preRefreshTime = driver.findElement(By.tagName("h2"))!!.text
        driver.navigate().refresh()
        driver.assertOnPage("/bill")
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
    fun `click`() {
        driver.get("/bill")
        driver.findElement(By.tagName("a"))!!.click()
        driver.assertOnPage("/link")
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

    private fun WebDriver.assertOnPage(expected: String) {
        assertThat(this.findElement(By.tagName("h1"))!!.text, equalTo(expected))
    }

}