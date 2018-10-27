package org.http4k.k8s

import org.http4k.core.Uri
import org.http4k.lens.*

data class K8sEnvironment private constructor(private val env: Map<String, String>) {
    internal operator fun get(key: String) = env[key.convertToKey()]
    internal operator fun set(key: String, value: String): K8sEnvironment = copy(env = env + (key.convertToKey() to value))

    private fun String.convertToKey() = toUpperCase().replace("-", "_")

    companion object {
        val ENV = from(System.getenv())
        val EMPTY = from(emptyMap())
        fun from(env: Map<String, String>) = K8sEnvironment(env)
    }
}

object K8sEnvKey : BiDiLensSpec<K8sEnvironment, String>("env", ParamMeta.StringParam,
    LensGet { name, target -> target[name]?.let { listOf(it) } ?: emptyList() },
    LensSet { name, values, target -> values.fold(target) { acc, next -> acc.set(name, next) } }
) {
    val SERVICE_PORT = int().required("SERVICE_PORT")
    val HEALTH_PORT = int().required("HEALTH_PORT")

    fun serviceUriFor(serviceName: String, https: Boolean = false) = int()
        .map {
            Uri.of("/")
                .scheme(if (https) "https" else "http")
                .authority(if (it == 80 || it == 443) serviceName else "$serviceName:$it")
        }
        .required("${serviceName.replace("-", "_").toUpperCase()}_SERVICE_PORT")
}
