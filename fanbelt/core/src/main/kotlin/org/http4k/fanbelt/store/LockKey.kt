package org.http4k.fanbelt.store

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory

/**
 * What a write acts on within its tenant: an operation's rule key. Some storage locks it instead of the whole tenant.
 */
class LockKey private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<LockKey>(::LockKey)
}
