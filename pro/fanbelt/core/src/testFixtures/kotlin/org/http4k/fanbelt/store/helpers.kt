/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store

data class Note(override val id: String, val topic: String, val author: String?, val size: Int) : Identified<String>

val notes = TableName.of("notes")

data class Tag(val topic: String, val n: Int) {
    override fun toString() = "$topic-$n"
}

data class Tagged(override val id: Tag, val size: Int) : Identified<Tag>

val tagged = TableName.of("tagged")
