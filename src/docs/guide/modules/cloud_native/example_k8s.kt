package guide.modules.cloud_native

import guide.modules.cloud_native.ProxyApp.Settings.dbRole
import guide.modules.cloud_native.ProxyApp.Settings.otherServiceUri
import org.http4k.client.JavaHttpClient
import org.http4k.cloudnative.Http4kK8sServer
import org.http4k.cloudnative.asK8sServer
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.cloudnative.health.Completed
import org.http4k.cloudnative.health.Health
import org.http4k.cloudnative.health.ReadinessCheck
import org.http4k.cloudnative.health.ReadinessCheckResult
import org.http4k.core.*
import org.http4k.filter.ClientFilters
import org.http4k.filter.DebuggingFilters
import org.http4k.lens.Lens
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import kotlin.random.Random

// this is a database client that we are going to health check
class RandomlyFailingDatabase(private val user: String) {
    fun insertARecord() {
        if (Random(1).nextBoolean()) throw Exception("oh no! $user has no access")
    }
}

// implements the check which will determine if this service is ready to go
class DatabaseCheck(private val db: RandomlyFailingDatabase) : ReadinessCheck {
    override val name = "database"
    override fun invoke(): ReadinessCheckResult {
        db.insertARecord()
        return Completed(name)
    }
}

object ProxyApp {

    object Settings {
        val otherServiceUri: Lens<Environment, Uri> = EnvironmentKey.k8s.serviceUriFor("otherservice")
        val dbRole = EnvironmentKey.required("database.user.role")
    }

    operator fun invoke(env: Environment): Http4kK8sServer {
        val proxyApp = ClientFilters.SetHostFrom(otherServiceUri(env))
            .then(rewriteUriToLocalhostAsWeDoNotHaveDns)
            .then(JavaHttpClient())

        return proxyApp.asK8sServer(::SunHttp, env, Health(checks = listOf(
            DatabaseCheck(RandomlyFailingDatabase(dbRole(env))))
        ))
    }

    private val rewriteUriToLocalhostAsWeDoNotHaveDns = Filter { next ->
        {
            println("Rewriting ${it.uri} so we can proxy properly")
            next(it.uri(it.uri.authority("localhost:9000")))
        }
    }
}

private fun performHealthChecks() {
    val client = DebuggingFilters.PrintResponse().then(JavaHttpClient())

    // health checks
    client(Request(Method.GET, "http://localhost:8001/liveness"))
    client(Request(Method.GET, "http://localhost:8001/readiness"))

    // proxied call
    client(Request(Method.GET, "http://localhost:8000"))
}

/** file app.properties contains
database.user.role=admin
 */

fun main(args: Array<String>) {
    val defaultConfig = Environment.defaults(
        EnvironmentKey.k8s.SERVICE_PORT of 8000,
        EnvironmentKey.k8s.HEALTH_PORT of 8001,
        EnvironmentKey.k8s.serviceUriFor("otherservice") of Uri.of("https://localhost:8000")
    )

    // standard chaining order for properties is local file -> JVM -> Environment -> defaults -> boom!
    val k8sPodEnv = Environment.fromResource("app.properties") overrides Environment.JVM_PROPERTIES overrides Environment.ENV overrides defaultConfig

    val upstream = { _: Request -> Response(Status.OK).body("HELLO!") }.asServer(SunHttp(9000)).start()

    val server = ProxyApp(k8sPodEnv).start()

    performHealthChecks()

    server.stop()
    upstream.stop()
}

