package org.reekwest.http.core.entity

import org.reekwest.http.core.HttpMessage
import java.nio.ByteBuffer

typealias Entity = ByteBuffer

interface EntityTransformer<T> {
    fun fromEntity(entity: Entity?): T
    fun toEntity(value: T): Entity
}

fun <T> HttpMessage.extract(entityExtractor: EntityTransformer<T>): T = entityExtractor.fromEntity(entity)