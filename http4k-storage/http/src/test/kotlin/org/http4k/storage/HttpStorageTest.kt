package org.http4k.storage

import org.http4k.contract.openapi.OpenAPIJackson.auto
import org.http4k.core.Body

class HttpStorageTest : StorageContract() {
    override val storage: Storage<AnEntity> =
        Storage.Http(
            Storage.InMemory<AnEntity>().asHttpHandler(),
            Body.auto<AnEntity>().toLens()
        )
}
