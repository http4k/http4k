package helpdesk

import helpdesk.feature.OrgPolicy
import org.http4k.connect.openfga.Http
import org.http4k.connect.openfga.OpenFga
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import java.time.Clock

fun orgPolicy(http: HttpHandler, clock: Clock = Clock.systemUTC()) = OrgPolicy.create(OpenFga.Http(Uri.of("http://openfga"), http), clock)
