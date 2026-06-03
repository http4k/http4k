package org.http4k.multipart

import java.io.Closeable
import java.io.File
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission.OWNER_READ
import java.nio.file.attribute.PosixFilePermission.OWNER_WRITE
import java.nio.file.attribute.PosixFilePermissions
import java.util.UUID

/**
 * Provides the management of the on-disk persistence for mulitpart files which are too big
 * to be processed in-memory.
 */
interface DiskLocation : Closeable {
    fun createFile(filename: String?): MultipartFile

    companion object {
        fun Temp(diskDir: File = Files.createTempDirectory("http4k-mp").toFile()) =
            object : DiskLocation {
                private val newFile = ownerOnlyTempFileFactory(diskDir)
                override fun createFile(filename: String?): MultipartFile = TempFile(newFile())

                override fun close() {
                    diskDir.listFiles()?.forEach { it.delete() }
                    diskDir.delete()
                }
            }

        fun Permanent(diskDir: File = Files.createTempDirectory("http4k-mp").toFile()) =
            object : DiskLocation {
                private val newFile = ownerOnlyTempFileFactory(diskDir)
                override fun createFile(filename: String?): MultipartFile = PermanentFile(newFile())

                override fun close() {}
            }
    }
}

private fun ownerOnlyTempFileFactory(diskDir: File): () -> File {
    val diskPath = diskDir.toPath()
    val suffix = ".tmp"
    if (diskPath.fileSystem.supportedFileAttributeViews().contains("posix")) {
        val ownerOnly = PosixFilePermissions.asFileAttribute(setOf(OWNER_READ, OWNER_WRITE))
        return { Files.createTempFile(diskPath, "${UUID.randomUUID()}-", suffix, ownerOnly).toFile() }
    }
    return {
        File.createTempFile("${UUID.randomUUID()}-", suffix, diskDir).apply {
            setReadable(false, false); setReadable(true, true)
            setWritable(false, false); setWritable(true, true)
        }
    }
}

interface MultipartFile : Closeable {
    fun file(): File
    override fun close()
}

internal class TempFile(private val file: File) : MultipartFile {

    override fun file() = file

    override fun close() {
        if (file.exists() && !file.delete()) throw FileSystemException("Failed to delete file")
    }
}

internal class PermanentFile(private val file: File) : MultipartFile {
    override fun file() = file

    override fun close() {
        // do nothing
    }
}
