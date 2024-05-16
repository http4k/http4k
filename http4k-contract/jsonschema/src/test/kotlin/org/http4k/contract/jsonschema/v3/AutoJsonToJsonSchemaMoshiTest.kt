package org.http4k.contract.jsonschema.v3

import org.http4k.format.Moshi
import org.http4k.format.MoshiNode

class AutoJsonToJsonSchemaMoshiTest : AutoJsonToJsonSchemaTest<MoshiNode> {

    override val json = Moshi

    override fun customJson() = json
}
