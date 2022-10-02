package blog.typesafe_configuration.post

import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.lens.Lens
import org.http4k.lens.duration
import org.http4k.lens.int
import java.time.Duration

data class Timeout(val value: Duration)
data class Port(val value: Int)
data class Host(val value: String)

// export TIMEOUT=PT30S
// export PORT=8080
// export HOSTNAMES=eu-west1.aws.com,eu-west2.aws.com,eu-west3.aws.com

val env = Environment.ENV

val portLens: Lens<Environment, Port> =
    EnvironmentKey.int().map(::Port).defaulted("PORT", Port(9000))
val timeoutLens: Lens<Environment, Timeout?> =
    EnvironmentKey.duration().map(::Timeout).optional("TIMEOUT")
val hostsLens: Lens<Environment, List<Host>> =
    EnvironmentKey.map(::Host).multi.required("HOSTNAMES")

val timeout: Timeout? = timeoutLens(env)
val port: Port = portLens(env)
val hosts: List<Host> = hostsLens(env)
