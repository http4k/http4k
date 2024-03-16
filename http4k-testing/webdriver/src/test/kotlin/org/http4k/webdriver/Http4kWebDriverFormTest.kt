package org.http4k.webdriver

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.MultipartFormBody
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.junit.jupiter.api.Test
import org.openqa.selenium.By
import java.io.File
import java.nio.file.Files
import kotlin.io.path.createTempFile

class Http4kWebDriverFormTest {
    private val driver = Http4kWebDriver({ req ->
        val body = File("src/test/resources/test.html").readText()
        Response(Status.OK).body(
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
    fun `POSTing a form prefixes with the original host in the URL`() {
        val driver = Http4kWebDriver(
            routes(
                "/submit" bind { Response(Status.OK).body(it.uri.toString()) },
                "/" bind { req ->
                    val body = File("src/test/resources/test.html").readText()
                    Response(Status.OK).body(
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
        assertThat(driver.pageSource, equalTo("http://host/submit"))
    }

    @Test
    fun `POST form`() {
        driver.get("https://example.com/bob")
        driver.findElement(By.id("button"))!!.submit()
        driver.assertOnPage("https://example.com/form")
        assertThat(driver, showsWeSentTheBody("text1=textValue&checkbox1=checkbox&textarea1=textarea&select1=option1&select1=option2&button=yes"))
        assertThat(driver, showsWeUsedTheMethod("POST"))
    }

    @Test
    fun `POST form via button click`() {
        driver.get("/bob")
        driver.findElement(By.id("resetbutton"))!!.click()
        driver.assertOnPage("/bob")
        driver.findElement(By.id("button"))!!.click()
        driver.assertOnPage("/form")
        assertThat(driver, showsWeSentTheBody("text1=textValue&checkbox1=checkbox&textarea1=textarea&select1=option1&select1=option2&button=yes"))
        assertThat(driver, showsWeUsedTheMethod("POST"))
    }

    @Test
    fun `POST form with empty action`() {
        var loadCount = 0
        val driver = Http4kWebDriver({ req ->
            loadCount++
            val body = File("src/test/resources/test.html").readText()
            Response(Status.OK).body(
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
        assertThat(driver, showsWeSentTheBody("text1=textValue&checkbox1=checkbox&textarea1=textarea&select1=option1&select1=option2&button=yes"))
        assertThat(driver, showsWeUsedTheMethod("POST"))
    }

    @Test
    fun `POST form with action set to empty string`() {
        var loadCount = 0
        val driver = Http4kWebDriver({ req ->
            loadCount++
            val body = File("src/test/resources/test.html").readText()
            Response(Status.OK).body(
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
        assertThat(driver, showsWeSentTheBody("text1=textValue&checkbox1=checkbox&textarea1=textarea&select1=option1&select1=option2&button=yes"))
        assertThat(driver, showsWeUsedTheMethod("POST"))
    }

    @Test
    fun `POST form with action set to fragment with no leading slash replaces last part of current base path`() {
        val driver = Http4kWebDriver({ req ->
            val body = File("src/test/resources/test.html").readText()
            Response(Status.OK).body(
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
        assertThat(driver, hasCurrentUrl("http://example.com/bob/was/here/fragmentWithNoLeadingSlash"))
    }


    @Test
    fun `GET form`() {
        val driver = Http4kWebDriver({ req ->
            val body = File("src/test/resources/test.html").readText()
            Response(Status.OK).body(
                body
                    .replace("FORMMETHOD", GET.name)
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

        assertThat(driver, showsWeSentTheBody(""))
        assertThat(driver, showsWeUsedTheMethod("GET"))
    }

    @Test
    fun `POST form with an empty text box`() {
        driver.get("https://example.com/bob")
        driver.findElement(By.tagName("textarea"))!!.sendKeys("")
        driver.findElement(By.id("button"))!!.submit()
        driver.assertOnPage("https://example.com/form")
        assertThat(driver, showsWeSentTheBody("text1=textValue&checkbox1=checkbox&textarea1=&select1=option1&select1=option2&button=yes"))
        assertThat(driver, showsWeUsedTheMethod("POST"))
    }


    // https://www.w3.org/TR/html401/interact/forms.html#h-17.13
    @Test
    fun `POST form via input of type 'submit' click`() {
        driver.get("https://example.com/bob")
        driver.findElement(By.id("input-submit"))!!.click()
        driver.assertOnPage("https://example.com/form")
        assertThat(driver, showsWeSentTheBody("text1=textValue&checkbox1=checkbox&textarea1=textarea&select1=option1&select1=option2"))
        assertThat(driver, showsWeUsedTheMethod("POST"))
    }

    @Test
    fun `POST form - activated submit buttons ('input' elements) are submitted with the form`() {
        driver.get("https://example.com/bob")
        driver.findElement(By.id("only-send-when-activated"))!!.submit()
        driver.assertOnPage("https://example.com/form")
        val expectedFormBody = "text1=textValue&checkbox1=checkbox&only-send-when-activated=only-send-when-activated&textarea1=textarea&select1=option1&select1=option2"

        assertThat(driver, showsWeSentTheBody(expectedFormBody))
        assertThat(driver, showsWeUsedTheMethod("POST"))
    }

    @Test
    fun `POST form - form elements associated with the form by the 'form' attribute are still sent`() {
        val driver = Http4kWebDriver({ req ->
            val body = File("src/test/resources/form_element_association.html").readText()

            Response(Status.OK).body(
                body.replace("THEBODY", req.bodyString())
                    .replace("THEURL", req.uri.toString())
            )
        })

        driver.get("https://example.com/bob")
        driver.findElement(By.id("button"))!!.click()
        driver.assertOnPage("https://example.com/form")
        val expectedFormBody = "text1=textValue&checkbox1=checkbox&textarea1=textarea&select1=option1&select1=option2&button=yes"

        assertThat(driver, showsWeSentTheBody(expectedFormBody))
    }

    @Test
    fun `POST form - a form that has an 'enctype' of 'multipart form-data' transmits its data as a multipart form`() {
        val driver = Http4kWebDriver({ req ->
            val body = File("src/test/resources/file_upload_test.html").readText()
            if (req.method == GET) return@Http4kWebDriver Response(Status.OK).body(body)

            val formBody = MultipartFormBody.from(req)
            val file = formBody.file("file")!!
            val otherFieldNames = listOf("text1", "textarea1", "checkbox1", "select1", "button")
            val pairsOfOtherFields =
                otherFieldNames.flatMap { fieldName -> formBody.fieldValues(fieldName).map { fieldName to it } }
            val otherFieldsString =
                pairsOfOtherFields.map { (fieldName, value) -> "$fieldName=$value" }.joinToString("&")

            Response(Status.OK).body(
                body
                    .replace("ENCODING", req.header("content-type")!!)
                    .replace("FILENAME", file.filename)
                    .replace("FILECONTENT", file.content.asString())
                    .replace("OTHERFORMFIELDS", otherFieldsString)
            )
        })

        val fileContent = "hello mum"
        val filePath = createTempFile("file-upload-test", ".txt")
        Files.newBufferedWriter(filePath).use { it.write(fileContent) }

        driver.get("https://example.com/bob")
        driver.findElement(By.cssSelector("input[type=file]"))!!.sendKeys(filePath.toString())
        driver.findElement(By.tagName("button"))!!.submit()

        assertThat(driver, hasElement(By.tagName("theformencoding"), hasText(startsWith("multipart/form-data"))))
        assertThat(driver, hasElement(By.tagName("thefilename"), hasText(equalTo(filePath.fileName.toString()))))
        assertThat(driver, hasElement(By.tagName("thefilecontent"), hasText(equalTo(fileContent))))

        val expectedOtherFields =
            "text1=textValue&textarea1=textarea&checkbox1=checkbox&select1=option1&select1=option2&button=yes"

        assertThat(driver, hasElement(By.tagName("theotherformfields"), hasText(equalTo(expectedOtherFields))))
    }
}

private fun showsWeSentTheBody(body: String) = hasElement(By.tagName("thebody"), hasText(equalTo(body)))
private fun showsWeUsedTheMethod(method: String) = hasElement(By.tagName("themethod"), hasText(equalTo(method)))
