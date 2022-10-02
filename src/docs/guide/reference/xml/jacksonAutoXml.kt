package guide.reference.xml

import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.format.JacksonXml.auto

data class JacksonWrapper(val message: JacksonMsg?)

data class JacksonMsg(
    val subject: String?,
    val from: String?,
    val to: String?,
    val content: String?
)

fun main() {
    // We can use the auto method here from the JacksonXML message format object. Note that the
    // auto() method is an extension function which needs to be manually imported (IntelliJ won't pick it up automatically).
    val messageLens = Body.auto<JacksonWrapper>().toLens()

    // extract the body from the message - this also works with Response
    val wrapper = JacksonWrapper(JacksonMsg("subject", "from", "to", "content"))
    val message =
        """<jacksonWrapper><message subject="hi"><from>david@http4k.org</from><to>ivan@http4k.org</to>hello world</message></jacksonWrapper>"""

    println(messageLens(Request(GET, "/").body(message)))

    // inject a converted object-as-XML-string into a request
    println(Request(GET, "").with(messageLens.of(wrapper)).bodyString())
}
