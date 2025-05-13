package ru.petsinbloom.jFerzl;

import java.io.*;
import java.nio.file.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipCsvExtractor {
    private static Consumer<String> output;

    public static void extractCsvFromZip(Consumer<String> _output, Path zipFilePath, Path outputDirectory, String unzipName) throws IOException {
        output = _output;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
            ZipEntry entry;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();

                if (name.toLowerCase().endsWith(".csv")) {
                    // Path outputPath = outputDirectory.resolve(Paths.get(name).getFileName());
                    Path outputPath = outputDirectory.resolve(unzipName);

                    try (OutputStream os = new FileOutputStream(outputPath.toFile())) {
                        zis.transferTo(os);
                    }

                    output.accept("CSV extracted: " + outputPath.getFileName());
                    return;
                }
            }

            output.accept("No .csv file found in archive: " + zipFilePath.getFileName());
        }
    }
}
