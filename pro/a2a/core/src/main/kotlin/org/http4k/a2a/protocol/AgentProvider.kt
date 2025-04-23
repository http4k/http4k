package org.http4k.a2a.protocol

import org.http4k.a2a.protocol.model.OrgName
import org.http4k.core.Uri

data class AgentProvider(val organization: OrgName, val url: Uri? = null)
