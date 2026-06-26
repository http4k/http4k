package org.http4k.security

import org.http4k.core.Filter
import org.http4k.core.NoOp

/**
 * Mutual TLS (client certificate) security. The mTLS handshake is enforced at the transport/server
 * layer, so the filter defaults to a no-op.
 */
class MutualTLSSecurity(override val filter: Filter = Filter.NoOp, val name: String = "mutualTLS") : Security {
    companion object
}
