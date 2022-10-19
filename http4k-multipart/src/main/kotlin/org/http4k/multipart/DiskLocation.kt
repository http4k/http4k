package org.http4k.multipart

import java.io.Closeable
import java.io.File
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.util.*

interface DiskLocation {
    fun createFile(filename: String?) : MultipartFile
}

interface MultipartFile : Closeable {
    fun file(): File
    override fun close()
}

class TempDiskLocation(private val diskDir: File = Files.createTempDirectory("http4k-mp").toFile().apply { deleteOnExit() }) : DiskLocation {
    override fun createFile(filename: String?): MultipartFile =
        TempFile(File.createTempFile(
            filename ?: (UUID.randomUUID().toString() + "-"),
            ".tmp", diskDir
        ).apply {
            deleteOnExit()
        })
}

class PermanentDiskLocation(private val diskDir: File = Files.createTempDirectory("http4k-mp").toFile()) : DiskLocation {
    override fun createFile(filename: String?): MultipartFile =
        PermanentFile(File.createTempFile(
            filename ?: (UUID.randomUUID().toString() + "-"),
            ".tmp", diskDir
        ))
}

internal class TempFile(private val file: File) : MultipartFile {

    override fun file() = file

    override fun close() {
        if (!file.delete()) throw FileSystemException("Failed to delete file")
    }
}

internal class PermanentFile(private val file: File) : MultipartFile {

    override fun file() = file

    override fun close() {
        // do nothing
    }
}
