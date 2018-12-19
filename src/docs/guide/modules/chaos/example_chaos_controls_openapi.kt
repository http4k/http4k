package guide.modules.chaos

import org.http4k.chaos.ChaosBehaviours.ReturnStatus
import org.http4k.chaos.ChaosTriggers.PercentageBased
import org.http4k.chaos.appliedWhen
import org.http4k.chaos.withChaosControls
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.filter.ServerFilters.Cors
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    Cors(UnsafeGlobalPermissive)
        .then { Response(OK).body("A normal response") }
        .withChaosControls(ReturnStatus(INTERNAL_SERVER_ERROR).appliedWhen(PercentageBased(50)))
        .asServer(SunHttp(9000))
        .start()
        .also { println("Visit the app at http://localhost:9000 or see the OpenApi at https://www.http4k.org/swagger2/?url=http://localhost:9000/chaos") }
        .block()
}
