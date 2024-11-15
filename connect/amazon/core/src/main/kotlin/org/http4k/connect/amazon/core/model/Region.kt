package org.http4k.connect.amazon.core.model

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue

class Region private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<Region>(::Region) {
        val AF_SOUTH_1 = Region.of("af-south-1")
        val AP_EAST_1 = Region.of("ap-east-1")
        val AP_NORTHEAST_1 = Region.of("ap-northeast-1")
        val AP_NORTHEAST_2 = Region.of("ap-northeast-2")
        val AP_NORTHEAST_3 = Region.of("ap-northeast-3")
        val AP_SOUTH_1 = Region.of("ap-south-1")
        val AP_SOUTHEAST_1 = Region.of("ap-southeast-1")
        val AP_SOUTHEAST_2 = Region.of("ap-southeast-2")
        val CA_CENTRAL_1 = Region.of("ca-central-1")
        val CN_NORTH_1 = Region.of("cn-north-1")
        val CN_NORTHWEST_1 = Region.of("cn-northwest-1")
        val EU_CENTRAL_1 = Region.of("eu-central-1")
        val US_WEST_1 = Region.of("us-west-1")
        val US_WEST_2 = Region.of("us-west-2")
        val EU_NORTH_1 = Region.of("eu-north-1")
        val EU_SOUTH_1 = Region.of("eu-south-1")
        val EU_WEST_1 = Region.of("eu-west-1")
        val EU_WEST_2 = Region.of("eu-west-2")
        val EU_WEST_3 = Region.of("eu-west-3")
        val SA_EAST_1 = Region.of("sa-east-1")
        val ME_SOUTH_1 = Region.of("me-south-1")
        val US_EAST_1 = Region.of("us-east-1")
        val US_EAST_2 = Region.of("us-east-2")
    }
}
