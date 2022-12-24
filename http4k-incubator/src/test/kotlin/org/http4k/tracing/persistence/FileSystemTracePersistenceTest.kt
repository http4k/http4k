package org.http4k.tracing.persistence

import org.http4k.tracing.TracePersistence
import java.nio.file.Files.createTempDirectory

class FileSystemTracePersistenceTest : TracePersistenceContract {
    override val persistence = TracePersistence.FileSystem(createTempDirectory("FileSystemTracePersistenceTest").toFile())
}
