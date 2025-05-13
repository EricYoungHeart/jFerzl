package ru.petsinbloom.jFerzl;

import java.io.*;
import java.nio.file.*;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MultiCsvExtractor {
    private static Consumer<String> output;

    public static void extractNamedCsvParts(Consumer<String> _output,  Path zipFilePath, Path outputDirectory) throws IOException {
        output = _output;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
            ZipEntry entry;
            int csvCount = 0;

            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entryName.toLowerCase().endsWith(".csv") && entryName.contains("_")) {
                    // Extract suffix after the first underscore
                    String[] parts = entryName.split("_", 2);
                    if (parts.length == 2) {
                        String outputFileName = parts[1]; // e.g. "addr.csv"
                        Path outputPath = outputDirectory.resolve(outputFileName);

                        try (OutputStream os = new FileOutputStream(outputPath.toFile())) {
                            zis.transferTo(os);
                        }

                        output.accept("Extracted: " + outputFileName);
                        csvCount++;
                    }
                }
            }

            output.accept("Total CSV files extracted: " + csvCount);
        }
    }
}
