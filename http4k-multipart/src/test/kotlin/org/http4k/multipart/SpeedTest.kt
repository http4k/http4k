package org.http4k.multipart

import org.http4k.multipart.StreamingMultipartFormHappyTests.CR_LF
import org.junit.Ignore
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.PrintWriter
import java.nio.charset.StandardCharsets.UTF_8
import java.util.ArrayList
import java.util.Random
import kotlin.Comparator

class SpeedTest {

    @Test
    @Ignore
    @Throws(Exception::class)
    fun uploadMultipleFilesAndFields() {
        val results = PrintWriter(File("./out/results.csv"))
        val bigFile = File.createTempFile("uploadMultipleFilesAndFields-", ".bin", File("./out"))
        bigFile.deleteOnExit()

        val fileSizes = intArrayOf(1, 3, 10, 30, 100, 300, 1000, 30000, 100000, 300000, 1000000)
        val fileCounts = intArrayOf(1, 3, 10, 30, 100, 300, 1000, 30000, 100000, 300000, 1000000)
        val boundary = "-----hghdjhebvjgbg"
        val fullBoundary = "--" + boundary
        val r = Random()

        for (fileSizeIndex in fileSizes.indices) {
            for (fileCountsIndex in fileCounts.indices) {
                val fileCount = fileCounts[fileCountsIndex]
                val fileSize = fileSizes[fileSizeIndex]

                timeFile(bigFile, boundary, fullBoundary, r, fileCount.toLong(), fileSize.toLong(), results)
            }
        }
        results.close()

    }

    @Throws(IOException::class)
    private fun timeFile(bigFile: File, boundary: String, fullBoundary: String, r: Random, fileCount: Long, fileSize: Long, results: PrintWriter) {
        val approximateFileSize = fileSize * fileCount + fileCount * 129
        if (approximateFileSize > 150000000) {
            println("File outside size constraints $fileCount * $fileSize => $approximateFileSize")
            return
        }

        println(">>>>>>> TIMING $fileCount files of size $fileSize")

        PrintWriter(bigFile).use { writer ->
            for (j in 0..fileCount - 1) {
                writer.print(fullBoundary)
                writer.print(CR_LF)
                writer.print("Content-Disposition: form-data; name=\"file$j\"; filename=\"foo.tab\"")
                writer.print(CR_LF)
                writer.print("Content-Type: text/whatever")
                writer.print(CR_LF)
                writer.print(CR_LF)

                for (i in 0..fileSize - 1) {
                    writer.write(r.nextInt() and 0x7F)
                }

                writer.print(CR_LF)
                if (j % 1000 == 0L) {
                    print(".")
                }
            }
            writer.print(fullBoundary)
            writer.print("--")
            writer.print(CR_LF)
        }
        println()
        println("file created " + bigFile.length())

        if (bigFile.length() < 500000 || bigFile.length() > 150000000) {
            println("File outside size constraints " + bigFile.length())
            return
        }

        val timings = ArrayList<Long>(200)
        val endTime = System.currentTimeMillis() + 30000

        var i = 0
        while (System.currentTimeMillis() < endTime && i < 100) {
            FileInputStream(bigFile).use { inputStream ->
                val start = System.currentTimeMillis()
                val form = StreamingMultipartFormParts.parse(
                    boundary.toByteArray(UTF_8),
                    inputStream,
                    UTF_8)

                for (part in form) {
                    val fieldName = part.fieldName
                }
                val end = System.currentTimeMillis()
                //                System.out.println("Finished in " + (end - start));
                timings.add(end - start)
            }
            i++
        }

        timings.sortWith(Comparator { obj, anotherLong -> obj.compareTo(anotherLong) })
        val high = timings[timings.size - 1]
        val median = timings[(timings.size - 1) / 2]
        val low = timings[0]
        val ms_per_file_bytes = median as Double * 10000000 / (fileCount * fileSize)
        val ms_per_stream_bytes = median as Double * 10000000 / bigFile.length()

        println("Samples " + timings.size)
        println("High   " + high)
        println("Median " + median)
        println("  ms/file bytes   " + ms_per_file_bytes)
        println("  ms/stream bytes " + ms_per_stream_bytes)
        println("Low    " + low)

        results.println(
            fileCount.toString() + "," +
                fileSize + "," +
                bigFile.length() + "," +
                timings.size + "," +
                high + "," +
                median + "," +
                low + "," +
                ms_per_file_bytes + "," +
                ms_per_stream_bytes
        )
        results.flush()

        bigFile.delete()

    }

    companion object {
        val TEMPORARY_FILE_DIRECTORY = File("./out/tmp")

        init {
            TEMPORARY_FILE_DIRECTORY.mkdirs()
        }
    }

    /*
    Results @ 628d0f8db98f03aadb7941f7d30f4a6009650f64
    1M * 1 ->     1,000,145 bytes
        Median 19 (18.997 ms/1M bytes)
        Low    15
    1K * 1K ->    1,122,914 bytes
        Median 29 (25.826 ms/1M bytes)
        Low    21
    100 * 10K ->  2,238,914 bytes
        Median 84 (37.518 ms/1M bytes)
        Low    66
    50 * 20K ->   3,488,914 bytes
        Median 167 (47.866 ms/1M bytes)
        Low    124
    10 * 100K -> 13,488,914 bytes
        Median 781 (57.899 ms/1M bytes)
        Low    606
    7 * 142857 -> 18,888,895 bytes
        Median 1005 (53.2xx ms/1M bytes)
        Low    850
    5 * 200K ->  26,088,914 bytes
        Median 1366 (52.359 ms/1M bytes)
        Low    1219
    1 * 1M ->   126,888,914 bytes
        Median 644 (5.075 ms/1M bytes)
        Low    591

     */

}
