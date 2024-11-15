package org.http4k.core

@Suppress("UNCHECKED_CAST")
fun <T : HttpMessage> T.alphabetiseHeaders(): T = headers
    .groupBy { it.first }
    .toSortedMap()
    .mapValues { it to it.value.sortedBy { it.second } }
    .asSequence()
    .fold(this) { acc, toRemove ->
        toRemove.value.second.fold(acc.removeHeader(toRemove.key) as T) { t, toAdd ->
            t.header(toAdd.first, toAdd.second) as T
        }
    }
