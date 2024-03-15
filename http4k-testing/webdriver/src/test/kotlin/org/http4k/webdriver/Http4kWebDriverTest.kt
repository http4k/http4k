package org.http4k.webdriver

import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import com.natpryce.hamkrest.startsWith
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.MultipartFormBody
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SEE_OTHER
import org.http4k.core.Uri
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import org.openqa.selenium.Cookie
import org.openqa.selenium.SearchContext
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement
import java.io.File
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.time.Instant
import java.util.Date
import org.http4k.core.cookie.Cookie as HCookie

class Http4kWebDriverTest {
    private val driver = Http4kWebDriver({ req ->
        val body = File("src/test/resources/test.html").readText()
        Response(OK).body(
            body
                .replace("FORMMETHOD", POST.name)
                .replace("THEMETHOD", req.method.name)
                .replace("THEBODY", req.bodyString())
                .replace("THEURL", req.uri.toString())
                .replace("THETIME", System.currentTimeMillis().toString())
                .replace("ACTION", "action=\"/form\"")
        )
    })

    @Test
    fun `page details`() {
        driver.get("/bob")
        assertThat(driver.currentUrl, equalTo("/bob"))
        assertThat(driver.title, equalTo("Page title"))
        assertThat(driver.findElement(By.id("firstId"))!!.text, equalTo("the first text"))
    }

    @Test
    fun `POSTing a form prefixes with the original host in the URL`() {
        val driver = Http4kWebDriver(
            routes(
                "/submit" bind { Response(OK).body(it.uri.toString()) },
                "/" bind { req ->
                    val body = File("src/test/resources/test.html").readText()
                    Response(OK).body(
                        body
                            .replace("FORMMETHOD", POST.name)
                            .replace("THEMETHOD", req.method.name)
                            .replace("THEBODY", req.bodyString())
                            .replace("THEURL", req.uri.toString())
                            .replace("THETIME", System.currentTimeMillis().toString())
                            .replace("ACTION", """action="/submit"""")
                    )
                })
        )
        driver.navigate().to(Uri.of("http://host/"))
        driver.findElement(By.id("button"))!!.submit()
        assertThat(
            driver.pageSource,
            equalTo("http://host/submit")
        )
    }

    @Test
    fun `POST form`() {
        driver.get("https://example.com/bob")
        driver.findElement(By.id("button"))!!.submit()
        driver.assertOnPage("https://example.com/form")
        assertThat(
            driver.findElement(By.tagName("thebody"))!!.text,
            equalTo("text1=textValue&checkbox1=checkbox&textarea1=textarea&select1=option1&select1=option2&button=yes")
        )
        assertThat(driver.findElement(By.tagName("themethod"))!!.text, equalTo("POST"))
    }

    @Test
    fun `POST form via button click`() {
        driver.get("/bob")
        driver.findElement(By.id("resetbutton"))!!.click()
        driver.assertOnPage("/bob")
        driver.findElement(By.id("button"))!!.click()
        driver.assertOnPage("/form")
        assertThat(
            driver.findElement(By.tagName("thebody"))!!.text,
            equalTo("text1=textValue&checkbox1=checkbox&textarea1=textarea&select1=option1&select1=option2&button=yes")
        )
        assertThat(driver.findElement(By.tagName("themethod"))!!.text, equalTo("POST"))
    }

    @Test
    fun `POST form with empty action`() {
        var loadCount = 0
        val driver = Http4kWebDriver({ req ->
            loadCount++
            val body = File("src/test/resources/test.html").readText()
            Response(OK).body(
                body
                    .replace("FORMMETHOD", POST.name)
                    .replace("THEMETHOD", req.method.name)
                    .replace("THEBODY", req.bodyString())
                    .replace("THEURL", req.uri.toString())
                    .replace("THETIME", System.currentTimeMillis().toString())
                    .replace("ACTION", "action")
            )
        })

        val n0 = loadCount
        driver.get("http://example.com/bob")
        driver.findElement(By.id("button"))!!.submit()
        driver.assertOnPage("http://example.com/bob")
        assertThat(loadCount, equalTo(n0 + 2))
        assertThat(
            driver.findElement(By.tagName("thebody"))!!.text,
            equalTo("text1=textValue&checkbox1=checkbox&textarea1=textarea&select1=option1&select1=option2&button=yes")
        )
        assertThat(driver.findElement(By.tagName("themethod"))!!.text, equalTo("POST"))
    }

    @Test
    fun `POST form with action set to empty string`() {
        var loadCount = 0
        val driver = Http4kWebDriver({ req ->
            loadCount++
            val body = File("src/test/resources/test.html").readText()
            Response(OK).body(
                body
                    .replace("FORMMETHOD", POST.name)
                    .replace("THEMETHOD", req.method.name)
                    .replace("THEBODY", req.bodyString())
                    .replace("THEURL", req.uri.toString())
                    .replace("THETIME", System.currentTimeMillis().toString())
                    .replace("ACTION", "action=\"\"")
            )
        })
        val n0 = loadCount
        driver.get("http://127.0.0.1/bob")
        driver.findElement(By.id("button"))!!.submit()
        driver.assertOnPage("http://127.0.0.1/bob")
        assertThat(loadCount, equalTo(n0 + 2))
        assertThat(
            driver.findElement(By.tagName("thebody"))!!.text,
            equalTo("text1=textValue&checkbox1=checkbox&textarea1=textarea&select1=option1&select1=option2&button=yes")
        )
        assertThat(driver.findElement(By.tagName("themethod"))!!.text, equalTo("POST"))
    }

    @Test
    fun `POST form with action set to fragment with no leading slash replaces last part of current base path`() {
        val driver = Http4kWebDriver({ req ->
            val body = File("src/test/resources/test.html").readText()
            Response(OK).body(
                body
                    .replace("FORMMETHOD", POST.name)
                    .replace("THEMETHOD", req.method.name)
                    .replace("THEBODY", req.bodyString())
                    .replace("THEURL", req.uri.toString())
                    .replace("THETIME", System.currentTimeMillis().toString())
                    .replace("ACTION", "action=\"fragmentWithNoLeadingSlash\"")
            )
        })

        driver.get("http://example.com/bob/was/here/today")
        driver.findElement(By.id("button"))!!.submit()
        driver.assertCurrentUrl("http://example.com/bob/was/here/fragmentWithNoLeadingSlash")
    }


    @Test
    fun `GET form`() {
        val driver = Http4kWebDriver({ req ->
            val body = File("src/test/resources/test.html").readText()
            Response(OK).body(
                body
                    .replace("FORMMETHOD", Method.GET.name)
                    .replace("THEMETHOD", req.method.name)
                    .replace("THEBODY", req.bodyString())
                    .replace("THEURL", req.uri.toString())
                    .replace("THETIME", System.currentTimeMillis().toString())
                    .replace("ACTION", "action=\"/form\"")
            )
        })

        driver.get("/bob")
        driver.findElement(By.id("button"))!!.submit()
        driver.assertOnPage("/form?text1=textValue&checkbox1=checkbox&textarea1=textarea&select1=option1&select1=option2&button=yes")
        assertThat(driver.findElement(By.tagName("thebody"))!!.text, equalTo(""))
        assertThat(driver.findElement(By.tagName("themethod"))!!.text, equalTo("GET"))
    }

    @Test
    fun navigation() {
        driver.navigate().to("https://localhost/rita")
        driver.assertOnPage("https://localhost/rita")
        assertThat(driver.currentUrl, equalTo("https://localhost/rita"))
        driver.navigate().to(URI.create("http://localhost/bob").toURL())
        driver.assertOnPage("http://localhost/bob")
        assertThat(driver.currentUrl, equalTo("http://localhost/bob"))

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
        driver.get(Uri.of("https://localhost/rita"))
        driver.assertOnPage("https://localhost/rita")
        driver.get("/bill")
        driver.assertOnPage("/bill")
        driver.navigate().to(Uri.of("https://localhost/rita"))
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
    fun `normalise links when clicking`() {
        assertLinkGoesTo("/bill", By.tagName("a"), "/link")
        assertLinkGoesTo("/bill", By.id("noPath"), "/bill")
        assertLinkGoesTo("/bill", By.id("sameDirPath"), "/bill/bob")
        assertLinkGoesTo("/bill", By.id("backForwardPath"), "/bob/link")
        assertLinkGoesTo("/bill", By.id("backPath"), "/")
        assertLinkGoesTo("/bill", By.id("dotPath"), "/bill/bob/link")
        assertLinkGoesTo("/bill", By.id("dotBackPath"), "/bob/link")
        assertLinkGoesTo("/bill", By.id("rootBackPath"), "/bob/link")
        assertLinkGoesTo("/", By.tagName("a"), "/link")
        assertLinkGoesTo("/", By.id("noPath"), "/")
        assertLinkGoesTo("/", By.id("sameDirPath"), "/bob")
        assertLinkGoesTo("/", By.id("backForwardPath"), "/bob/link")
        assertLinkGoesTo("/", By.id("backPath"), "/")
        assertLinkGoesTo("/", By.id("dotPath"), "/bob/link")
        assertLinkGoesTo("/", By.id("dotBackPath"), "/bob/link")
        assertLinkGoesTo("/", By.id("rootBackPath"), "/bob/link")
    }

    private fun assertLinkGoesTo(initial: String, by: By, expected: String) {
        driver.get(initial)
        driver.findElement(by)!!.click()
        driver.assertOnPage(expected)
    }

    @Test
    fun `cookie adding and deleting`() {
        val cookie1 = Cookie("foo1", "bar")
        val cookie2 = Cookie("foo2", "bar")
        val cookie3 = Cookie("foo3", "bar")

        assertThat(driver.manage().cookies, equalTo(emptySet()))
        driver.manage().addCookie(cookie1)
        driver.manage().addCookie(cookie2)
        driver.manage().addCookie(cookie3)
        assertThat(driver.manage().cookies, equalTo(setOf(cookie1, cookie2, cookie3)))
        assertThat(driver.manage().getCookieNamed("foo1"), equalTo(cookie1))
        driver.manage().deleteCookieNamed("foo1")
        assertThat(driver.manage().cookies, equalTo(setOf(cookie2, cookie3)))
        driver.manage().deleteCookie(cookie2)
        assertThat(driver.manage().cookies, equalTo(setOf(cookie3)))
        driver.manage().deleteAllCookies()
        assertThat(driver.manage().cookies, equalTo(emptySet()))
    }

    @Test
    fun `cookies are added to request`() {
        val driver = Http4kWebDriver({ req ->
            Response(OK).body(req.cookies().joinToString("; \n") { it.name + "=" + it.value })
        })
        driver.manage().addCookie(Cookie("foo1", "bar1", "domain", "/", Date(0), true, true))
        driver.manage().addCookie(Cookie("foo2", "bar2"))

        driver.get("/")

        assertThat(driver.pageSource, equalTo("foo1=bar1; \nfoo2=bar2"))
    }

    @Test
    fun `service set cookies are stored in the driver`() {
        val driver = Http4kWebDriver({
            Response(OK).cookie(HCookie("name", "value", 100, Instant.EPOCH, "domain", "path", true, true))
        })

        driver.get("/")

        assertThat(
            driver.manage().cookies,
            equalTo(setOf(Cookie("name", "value", "domain", "path", Date(0), true, true)))
        )
    }

    @Test
    fun `unsupported features`() {
        driver.get("/bill")

        val windowHandle = driver.windowHandle

        isNotImplemented { driver.manage().logs() }
        isNotImplemented { driver.manage().timeouts() }
        isNotImplemented { driver.manage().window() }
        isNotImplemented { driver.switchTo().alert() }
        isNotImplemented { driver.switchTo().frame(0) }
        isNotImplemented { driver.switchTo().frame("bob") }
        isNotImplemented { driver.switchTo().frame(windowHandle) }
        isNotImplemented { driver.switchTo().parentFrame() }
    }

    @Test
    fun `POST form with an empty text box`() {
        driver.get("https://example.com/bob")
        driver.findElement(By.tagName("textarea"))!!.sendKeys("")
        driver.findElement(By.id("button"))!!.submit()
        driver.assertOnPage("https://example.com/form")
        assertThat(
            driver.findElement(By.tagName("thebody"))!!.text,
            equalTo("text1=textValue&checkbox1=checkbox&textarea1=&select1=option1&select1=option2&button=yes")
        )
        assertThat(driver.findElement(By.tagName("themethod"))!!.text, equalTo("POST"))
    }

    @Test
    fun `Set the host header when navigating to a URL`() {
        val driver = Http4kWebDriver({ req ->
            Response(OK).body(req.header("host") ?: "none")
        })

        driver.navigate().to("/baz")
        assertThat(driver.pageSource, equalTo("none"))
        driver.navigate().to("http://foo.com:5000/bar")
        driver.navigate().to("/baz")
        assertThat(driver.pageSource, equalTo("foo.com:5000"))
        driver.navigate().to("http://baz.com/")
        assertThat(driver.pageSource, equalTo("baz.com"))
    }

    @Test
    fun `Set the host header when redirecting to a URL`() {
        val driver = Http4kWebDriver({ req ->
            when (req.header("host")) {
                "redirect.com" -> Response(SEE_OTHER).header("location", "http://destination.com")
                else -> Response(OK).body(req.header("host") ?: "none")
            }
        })

        driver.navigate().to("http://redirect.com")
        assertThat(driver.pageSource, equalTo("destination.com"))
    }

    // https://www.w3.org/TR/html401/interact/forms.html#h-17.13
    @Test
    fun `POST form via input of type 'submit' click`() {
        driver.get("https://example.com/bob")
        driver.findElement(By.id("input-submit"))!!.click()
        driver.assertOnPage("https://example.com/form")
        assertThat(
            driver.findElement(By.tagName("thebody"))!!.text,
            equalTo("text1=textValue&checkbox1=checkbox&textarea1=textarea&select1=option1&select1=option2")
        )
        assertThat(driver.findElement(By.tagName("themethod"))!!.text, equalTo("POST"))
    }

    @Test
    fun `POST form - activated submit buttons ('input' elements) are submitted with the form`() {
        driver.get("https://example.com/bob")
        driver.findElement(By.id("only-send-when-activated"))!!.submit()
        driver.assertOnPage("https://example.com/form")
        assertThat(
            driver.findElement(By.tagName("thebody"))!!.text,
            equalTo("text1=textValue&checkbox1=checkbox&only-send-when-activated=only-send-when-activated&textarea1=textarea&select1=option1&select1=option2")
        )
        assertThat(driver.findElement(By.tagName("themethod"))!!.text, equalTo("POST"))
    }

    @Test
    fun `POST form - a form that has an 'enctype' of 'multipart form-data' transmits its data as a multipart form`() {
        val driver = Http4kWebDriver({ req ->
            val body = File("src/test/resources/file_upload_test.html").readText()
            if (req.method == GET) return@Http4kWebDriver Response(OK).body(body)

            val formBody = MultipartFormBody.from(req)
            val file = formBody.file("file")!!

            Response(OK).body(body
                .replace("ENCODING", req.header("content-type")!!)
                .replace("FILENAME", file.filename)
                .replace("FILECONTENT", file.content.asString()))
        })

        val fileContent = "hello mum"
        val filePath = kotlin.io.path.createTempFile("file-upload-test", ".txt")
        Files.newBufferedWriter(filePath).use { it.write(fileContent) }

        driver.get("https://example.com/bob")
        driver.findElement(By.tagName("input"))!!.sendKeys(filePath.toString())
        driver.findElement(By.tagName("button"))!!.submit()

        assertThat(driver, hasElement(By.tagName("theformencoding"), hasText(startsWith("multipart/form-data"))))
        assertThat(driver, hasElement(By.tagName("thefilename"), hasText(equalTo(filePath.fileName.toString()))))
        assertThat(driver, hasElement(By.tagName("thefilecontent"), hasText(equalTo(fileContent))))
    }
}

private fun InputStream.asString(): String {
    return reader().use { it.readText() }
}

private fun WebDriver.assertOnPage(expected: String) {
    assertThat(findElement(By.tagName("h1"))!!.text, equalTo(expected))
}

private fun Http4kWebDriver.assertCurrentUrl(expectedUrl: String) {
    assertThat(currentUrl, equalTo(expectedUrl))
}

private fun hasElement(by: By, matcher: Matcher<WebElement>): Matcher<SearchContext> = object : Matcher<SearchContext> {
    override val description: String = "has the element matching ${by} that " + matcher.description

    override fun invoke(actual: SearchContext): MatchResult {
        val element: WebElement? = actual.findElement(by)

        return when (element) {
            null -> MatchResult.Mismatch("could not find element")
            else -> matcher(element)
        }
    }
}

private fun hasText(matcher: Matcher<String>): Matcher<WebElement> = object : Matcher<WebElement> {
    override val description: String = "has the text content " + matcher.description

    override fun invoke(actual: WebElement): MatchResult {
        val text : String? = actual.text

        return when (text) {
            null -> MatchResult.Mismatch("could not find any text content")
            else -> matcher(text)
        }
    }
}
