package org.http4k.connect.openai.endpoints

import org.http4k.routing.ResourceLoader
import org.http4k.routing.static

internal fun ServeLogo() = static(ResourceLoader.Classpath("public"))
