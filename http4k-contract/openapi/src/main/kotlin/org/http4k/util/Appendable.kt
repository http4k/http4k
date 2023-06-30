package org.http4k.util

class Appendable<T>(val all: MutableList<T> = mutableListOf()) {
    operator fun plusAssign(t: T) {
        all += t
    }

    operator fun plusAssign(t: Collection<T>) {
        all += t
    }
}
