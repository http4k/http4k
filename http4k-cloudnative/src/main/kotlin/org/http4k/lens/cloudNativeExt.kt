package org.http4k.lens

import org.http4k.cloudnative.env.Authority
import org.http4k.cloudnative.env.Host
import org.http4k.cloudnative.env.Port
import org.http4k.cloudnative.env.Secret
import org.http4k.cloudnative.env.Timeout

fun StringBiDiMappings.host() = nonBlank().map(::Host, Host::value)
fun StringBiDiMappings.port() = int().map(::Port, Port::value)
fun StringBiDiMappings.authority() = nonBlank().map({ Authority(it) }, Authority::toString)

fun <IN : Any> BiDiLensSpec<IN, String>.secret() = nonEmptyString().bytes().map(::Secret)
fun <IN : Any> BiDiLensSpec<IN, String>.host() = map(StringBiDiMappings.host())
fun <IN : Any> BiDiLensSpec<IN, String>.port() = map(StringBiDiMappings.port())
fun <IN : Any> BiDiLensSpec<IN, String>.authority() = map(StringBiDiMappings.authority())
fun <IN : Any> BiDiLensSpec<IN, String>.timeout() = duration().map(::Timeout, Timeout::value)

val Header.HOST get() = Header.authority().optional("host")
