package org.http4k.format

object KondorJson : ConfigurableKondorJson({
    asConfigurable()
        .withStandardMappings()
        .done()
})


