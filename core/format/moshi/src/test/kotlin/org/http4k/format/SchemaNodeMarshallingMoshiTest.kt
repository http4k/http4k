package org.http4k.format

import org.http4k.contract.jsonschema.SchemaNodeMarshallingContract

class SchemaNodeMarshallingMoshiTest : SchemaNodeMarshallingContract<MoshiNode>(Moshi)
