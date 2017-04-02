package org.reekwest.http.core.stringentity

import org.reekwest.http.core.Entity
import org.reekwest.http.core.Response
import org.reekwest.http.core.entity

fun Response.entity(entity: String) = entity(Entity(entity))