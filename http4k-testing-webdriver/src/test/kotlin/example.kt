
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.webdriver.Http4kWebDriver
import org.openqa.selenium.By

/**
 * Simple demonstration of how to use the http4k web-driver to test http4k apps.
 */
fun main(args: Array<String>) {

    val app = routes(
        "/hello" bind GET to {
            Response(Status.OK).body("<html><title>hello</title></html>")
        }
    )

    val driver = Http4kWebDriver(app)

    driver.navigate().to("http://localhost:10000/hello")

    println(driver.title)
    println(driver.findElement(By.tagName("title")))

// prints:
//
// hello
// JSoupWebElement(element=<title>hello</title>)
}