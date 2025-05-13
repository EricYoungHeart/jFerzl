package ru.petsinbloom.jFerzl;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class PatientsWriter {

    public static void writeToCsv(Path csvPath, List<Patient> records) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvPath.toFile()))) {
            // Header
            writer.write("CaseNumber,FullName,DateOfBirth,SSN");
            writer.newLine();

            for (Patient record : records) {
                String line = String.format("%d,%s,%s,%s,%s,%s,%s",
                        record.getId(),
                        escape(record.getCaseNumber()),
                        escape(record.getFam()),
                        escape(record.getIm()),
                        escape(record.getOt()),
                        record.getDateOfBirth(),
                        escape(record.getSocialSecurityNumber()));
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\""); // Handle quotes inside fields
    }
}
