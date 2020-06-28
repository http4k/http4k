import org.http4k.cloudnative.env.Environment
import org.http4k.serverless.openWhiskClient

fun main() {
    val ow = Environment.openWhiskClient(false)

    ow.getAllNamespaces().forEach {
        println("Namespace: $it")
        ow.getAllPackages(it).forEach {
            println("\tPackage: ${it.name}")
            (it.actions ?: emptyList()).forEach {
                println("\t\t${it.name}")
            }
        }
        println("--")
    }
}
