package org.http4k.util

import java.lang.reflect.Proxy

inline fun <reified T> proxy(): T = Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, _, _ -> TODO("not implemented") } as T
