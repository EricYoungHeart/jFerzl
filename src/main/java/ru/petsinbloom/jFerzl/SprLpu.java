package ru.petsinbloom.jFerzl;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class SprLpu {

    public static List<MedCompany> readFromCsv(Path csvFile) throws IOException {
        List<MedCompany> list = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(csvFile)) {
            String line;
            boolean skipHeader = true;

            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }

                String[] tokens = line.split(";", -1);
                if (tokens.length >= 3) {
                    list.add(new MedCompany(tokens[0].trim(), tokens[1].trim(), tokens[2].trim()));
                }
            }
        }

        return list;
    }
}
