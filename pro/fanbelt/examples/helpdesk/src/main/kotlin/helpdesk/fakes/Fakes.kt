package helpdesk.fakes

import org.http4k.connect.openfga.FakeOpenFga
import org.http4k.core.HttpHandler

fun Fakes(provider: FakeEmailProvider, openFga: FakeOpenFga = FakeOpenFga()): HttpHandler =
    { request -> if (request.uri.path == "/send") provider(request) else openFga(request) }
