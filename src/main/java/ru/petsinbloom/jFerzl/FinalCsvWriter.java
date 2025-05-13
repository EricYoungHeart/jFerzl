package ru.petsinbloom.jFerzl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class FinalCsvWriter {

    public static void writeFinalFormat(Path outputPath, List<Patient> patients) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath.toFile()))) {
            // Write header
            writer.write("REQ_ID;ENP2;PCY_TYPE;PCY_SER;PCY_NUM;DUDL_CODE;DUDL_SER;DUDL_NUM;SNILS;DATR;OIP");
            writer.newLine();

            for (Patient p : patients) {
                String line = String.format(
                        "%d;%s;;;;;;;;;",
                        p.getId(),
                        safe(p.getEnp())
                );
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
