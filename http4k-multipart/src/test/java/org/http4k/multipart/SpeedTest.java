package org.http4k.multipart;

import org.http4k.multipart.part.StreamingPart;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.http4k.multipart.StreamingMultipartFormHappyTests.CR_LF;

public class SpeedTest {
    public static final File TEMPORARY_FILE_DIRECTORY = new File("./out/tmp");

    static {
        TEMPORARY_FILE_DIRECTORY.mkdirs();
    }

    @Test
    @Ignore
    public void uploadMultipleFilesAndFields() throws Exception {
        PrintWriter results = new PrintWriter(new File("./out/results.csv"));
        File bigFile = File.createTempFile("uploadMultipleFilesAndFields-", ".bin", new File("./out"));
        bigFile.deleteOnExit();

        int[] fileSizes = new int[]{1, 3, 10, 30, 100, 300, 1000, 30_000, 100_000, 300_000, 1_000_000};
        int[] fileCounts = new int[]{1, 3, 10, 30, 100, 300, 1000, 30_000, 100_000, 300_000, 1_000_000};
        String boundary = "-----hghdjhebvjgbg";
        String fullBoundary = "--" + boundary;
        Random r = new Random();

        for (int fileSizeIndex = 0; fileSizeIndex < fileSizes.length; fileSizeIndex++) {
            for (int fileCountsIndex = 0; fileCountsIndex < fileCounts.length; fileCountsIndex++) {
                int fileCount = fileCounts[fileCountsIndex];
                int fileSize = fileSizes[fileSizeIndex];

                timeFile(bigFile, boundary, fullBoundary, r, fileCount, fileSize, results);
            }
        }
        results.close();

    }

    private void timeFile(File bigFile, String boundary, String fullBoundary, Random r, long fileCount, long fileSize, PrintWriter results) throws IOException {
        long approximateFileSize = fileSize * fileCount + (fileCount * 129);
        if (approximateFileSize > 150_000_000) {
            System.out.println("File outside size constraints " + fileCount + " * " + fileSize + " => " + approximateFileSize);
            return;
        }

        System.out.println(">>>>>>> TIMING " + fileCount + " files of size " + fileSize);

        try (PrintWriter writer = new PrintWriter(bigFile)) {
            for (int j = 0; j < fileCount; j++) {
                writer.print(fullBoundary); writer.print(CR_LF);
                writer.print("Content-Disposition: form-data; name=\"file" + j + "\"; filename=\"foo.tab\""); writer.print(CR_LF);
                writer.print("Content-Type: text/whatever"); writer.print(CR_LF);
                writer.print(CR_LF);

                for (int i = 0; i < fileSize; i++) {
                    writer.write(r.nextInt() & 0x7F);
                }

                writer.print(CR_LF);
                if (j % 1000 == 0) {
                    System.out.print(".");
                }
            }
            writer.print(fullBoundary); writer.print("--"); writer.print(CR_LF);
        }
        System.out.println();
        System.out.println("file created " + bigFile.length());

        if (bigFile.length() < 500_000 || bigFile.length() > 150_000_000) {
            System.out.println("File outside size constraints " + bigFile.length());
            return;
        }

        List<Long> timings = new ArrayList<>(200);
        long endTime = System.currentTimeMillis() + 30_000;

        for (int i = 0; System.currentTimeMillis() < endTime && i < 100; i++) {
            try (FileInputStream inputStream = new FileInputStream(bigFile)) {
                long start = System.currentTimeMillis();
                Iterable<StreamingPart> form = StreamingMultipartFormParts.parse(
                    boundary.getBytes(UTF_8),
                    inputStream,
                    UTF_8);

                for (StreamingPart part : form) {
                    String fieldName = part.fieldName;
                }
                long end = System.currentTimeMillis();
//                System.out.println("Finished in " + (end - start));
                timings.add(end - start);
            }
        }

        timings.sort(Long::compareTo);
        Long high = timings.get(timings.size() - 1);
        Long median = timings.get((timings.size() - 1) / 2);
        Long low = timings.get(0);
        double ms_per_file_bytes = (double) median * 10_000_000 / (fileCount * fileSize);
        double ms_per_stream_bytes = (double) median * 10_000_000 / (bigFile.length());

        System.out.println("Samples " + timings.size());
        System.out.println("High   " + high);
        System.out.println("Median " + median);
        System.out.println("  ms/file bytes   " + ms_per_file_bytes);
        System.out.println("  ms/stream bytes " + ms_per_stream_bytes);
        System.out.println("Low    " + low);

        results.println(
            fileCount + "," +
                fileSize + "," +
                bigFile.length() + "," +
                timings.size() + "," +
                high + "," +
                median + "," +
                low + "," +
                ms_per_file_bytes + "," +
                ms_per_stream_bytes
        );
        results.flush();

        bigFile.delete();

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
