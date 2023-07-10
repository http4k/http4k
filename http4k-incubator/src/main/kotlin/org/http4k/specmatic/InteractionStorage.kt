package org.http4k.specmatic

import `in`.specmatic.core.NamedStub

fun interface InteractionStorage {
    fun store(stubs: List<NamedStub>)
}
