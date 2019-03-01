package guide.modules.webdriver

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.webdriver.By
import org.http4k.webdriver.Http4kWebDriver

fun main() {
    val app = routes(
        "/hello" bind GET to {
            Response(OK).body("<html><title>hello</title></html>")
        }
    )

    val driver = Http4kWebDriver(app)

    driver.navigate().to("http://localhost:10000/hello")

    println(driver.title)
    println(driver.findElement(By.tagName("title")))

// prints:
//
// hello
// <title>hello</title>
}
