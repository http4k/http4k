/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class Http4kVerifyExtension @Inject constructor(objects: ObjectFactory) {
    val failOnError: Property<Boolean> = objects.property(Boolean::class.java).convention(true)
    val publicKey: RegularFileProperty = objects.fileProperty()
    val keyListUrl: Property<String> = objects.property(String::class.java)
        .convention("https://http4k.org/.well-known/cosign-keys.json")
}
