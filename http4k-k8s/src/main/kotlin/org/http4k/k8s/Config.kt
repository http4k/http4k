package org.http4k.k8s

import org.http4k.lens.*

class K8sEnvironment private constructor(private val env: Map<String, String>) {
    operator fun get(key: String) = env[key]

    companion object {
        val ENV = from(System.getenv())
        fun from(env: Map<String, String>) = K8sEnvironment(env)
    }
}

object K8sEnvKey : BiDiLensSpec<K8sEnvironment, String>("env", ParamMeta.StringParam,
    LensGet { name, target -> target[name]?.let { listOf(it) } ?: emptyList() },
    LensSet { _, _, _ -> throw IllegalArgumentException("Cannot inject into environment") }
) {
    val SERVICE_PORT = int().required("SERVICE_PORT")
    val HEALTH_PORT = int().required("HEALTH_PORT")
    fun portFor(serviceName: String) = int().required("${serviceName.replace("-", "_").toUpperCase()}_SERVICE_PORT")
}
