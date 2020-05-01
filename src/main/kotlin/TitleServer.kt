import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response

class TitleServer : HttpHandler {
  override operator fun invoke(Request: Request): Response = Response(org.http4k.core.Status.OK)}
