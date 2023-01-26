package org.http4k.storage

import org.http4k.format.Moshi
import kotlin.io.path.createTempDirectory

class DiskStorageTest : StorageContract() {
    override val storage =
        Storage.Disk<AnEntity>(createTempDirectory("http4k").run { toFile().apply { deleteOnExit() } }, Moshi)
}

