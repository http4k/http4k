package org.http4k.format

object AwsCoreMoshi : ConfigurableMoshi(
    AwsMoshiBuilder(AwsCoreJsonAdapterFactory())
        .done()
)
