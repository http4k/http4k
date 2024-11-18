package org.http4k.lens

import org.http4k.config.Authority
import org.http4k.config.Host
import org.http4k.config.Port
import org.http4k.config.Secret
import org.http4k.config.Timeout

fun StringBiDiMappings.host() = nonBlank().map(::Host, Host::value)
fun StringBiDiMappings.port() = int().map(::Port, Port::value)
fun StringBiDiMappings.authority() = nonBlank().map({ Authority(it) }, Authority::toString)
fun StringBiDiMappings.secret() = nonEmpty().map({ Secret(it) }, { secret -> secret.use { it } })

fun <IN : Any> BiDiLensSpec<IN, String>.secret() = map(StringBiDiMappings.secret())
fun <IN : Any> BiDiLensSpec<IN, String>.host() = map(StringBiDiMappings.host())
fun <IN : Any> BiDiLensSpec<IN, String>.port() = map(StringBiDiMappings.port())
fun <IN : Any> BiDiLensSpec<IN, String>.authority() = map(StringBiDiMappings.authority())
fun <IN : Any> BiDiLensSpec<IN, String>.timeout() = duration().map(::Timeout, Timeout::value)

val Header.HOST get() = Header.authority().optional("host")
