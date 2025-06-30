package org.http4k.connect.amazon.route53.model

import org.http4k.connect.amazon.core.model.Region
import org.http4k.connect.amazon.core.model.VpcId

data class VpcConfig(
    val vpcId: VpcId,
    val vpcRegion: Region
)
