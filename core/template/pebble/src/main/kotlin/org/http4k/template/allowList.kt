package org.http4k.template

import io.pebbletemplates.pebble.PebbleEngine
import io.pebbletemplates.pebble.attributes.methodaccess.AllowlistMethodAccessValidator

/**
 * Allows templates to call methods on the default safe JDK types and on classes in the given packages.
 */
fun allowList(vararg packages: String): (PebbleEngine.Builder) -> PebbleEngine.Builder = {
    it.methodAccessValidator(
        AllowlistMethodAccessValidator.builder().allowDefaults().allowPackages(*packages).build()
    )
}
