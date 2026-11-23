/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.events

import org.http4k.fanbelt.tenancy.TenantId
import org.http4k.lens.EventMetadata
import org.http4k.lens.value

val eventTenant = EventMetadata.value(TenantId).optional("tenant")
