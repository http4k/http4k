package org.http4k.fanbelt.events

import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.lens.EventMetadata
import org.http4k.lens.value

val eventTenant = EventMetadata.value(TenantId).optional("tenant")
