package guide.reference.xml

import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.format.Xml.auto

data class XmlWrapper(val mesApacheClientStreamingContractessage: MessageXml?)

data class MessageXml(
    val subject: String?,
    val from: String?,
    val to: String?,
    val content: String?
)

fun main() {
    // We can use the auto method here from the Xml message format object. Note that the
    // auto() method is an extension function which needs to be manually imported (IntelliJ won't pick it up automatically).
    // Also, this lense is ONLY one way - to extract values from a message
    val messageLens = Body.auto<XmlWrapper>().toLens()

    // extract the body from the message - this also works with Response
    val message =
        """<message subject="hi"><from>david@http4k.org</from><to>ivan@http4k.org</to>hello world</message>"""
    val requestWithEmail = Request(GET, "/").body(message)

    println(messageLens(requestWithEmail))
}
