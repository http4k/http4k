package org.http4k.connect.amazon.sts.model

import org.http4k.connect.amazon.core.model.ARN

data class AssumedRoleUser(val Arn: ARN, val AssumedRoleId: RoleId)
