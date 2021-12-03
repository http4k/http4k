package org.http4k.core

import org.http4k.cloudnative.env.Authority
import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.Host
import org.http4k.cloudnative.env.Port

fun Uri.port(port: Port?) = port(port?.value)
fun Uri.port() = port?.let(::Port)

fun Uri.host() = Host(host)
fun Uri.host(host: Host) = host(host.value)

fun Uri.authority(authority: Authority) = host(authority.host).port(authority.port)
fun Uri.authority() = Authority(host(), port())

fun Environment.with(vararg modifiers: (Environment) -> Environment) = modifiers.fold(this) { memo, next -> next(memo) }
