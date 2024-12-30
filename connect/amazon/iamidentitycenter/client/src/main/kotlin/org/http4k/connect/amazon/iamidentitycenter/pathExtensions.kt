package org.http4k.connect.amazon.iamidentitycenter

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.createFile


val POSIX_OWNER_ONLY_FILE = setOf(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE)

fun Path.hasPosixFilePermissions() = "posix" in this.fileSystem.supportedFileAttributeViews()

fun Path.touch(restrictToOwner: Boolean = false): Path = try {
    if (!restrictToOwner || !hasPosixFilePermissions()) {
        createFile()
    } else {
        createFile(PosixFilePermissions.asFileAttribute(POSIX_OWNER_ONLY_FILE))
    }
} catch (_: FileAlreadyExistsException) {
    this
}

