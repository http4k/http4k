package org.http4k.format

class MoshiAutoEventsTest : AutoMarshallingEventsContract(Moshi) {
    override fun extendedMarshaller() = ConfigurableMoshi(
        standardConfig()
            .customise()
            .add(ProtocolStatusAdapter)
    )
}
