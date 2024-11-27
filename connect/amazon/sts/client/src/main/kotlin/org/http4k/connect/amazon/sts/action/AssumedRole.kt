package org.http4k.connect.amazon.sts.action

import org.http4k.connect.amazon.core.model.Credentials
import org.http4k.connect.amazon.sts.model.RoleId

interface AssumedRole {
    val AssumedRoleId: RoleId
    val Credentials: Credentials
}
