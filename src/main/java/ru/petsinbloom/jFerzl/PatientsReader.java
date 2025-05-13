package ru.petsinbloom.jFerzl;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PatientsReader {

    public static List<Patient> readFromCsv(Path csvPath) throws IOException {
        List<Patient> records = new ArrayList<>();

        // try (BufferedReader reader = new BufferedReader(new FileReader(csvPath.toFile()))) {
        // Пробуем заменить на более современную реализацию
        try (BufferedReader reader = Files.newBufferedReader(csvPath, StandardCharsets.UTF_8)) { // Charset.forName("Windows-1251")
            String line;
            boolean skipHeader = true;

            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }

                String[] tokens = line.split(",", -1); // -1 = keep empty fields

                if (tokens.length < 7) {
                    System.err.println("Skipping malformed line: " + line);
                    continue;
                }

                long id = Long.parseLong(tokens[0].trim());
                String caseNumber = tokens[1].trim();
                String fam = capitalize(tokens[2].trim());
                String im = capitalize(tokens[3].trim());
                String ot = capitalize(tokens[4].trim());
                LocalDate dob = LocalDate.parse(tokens[5].trim());
                String ssn = tokens[6].trim();

                records.add(new Patient(id, caseNumber, fam, im, ot, dob, ssn));
            }
        }

        return records;
    }

    private static String capitalize(String input) {
        if (input == null || input.isBlank()) return "";
        input = input.toLowerCase();
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }
}
