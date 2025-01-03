package org.http4k.testing

fun main() {
    HotReloadServer.http<HttpApp>().start()
}
