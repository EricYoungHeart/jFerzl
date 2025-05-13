package ru.petsinbloom.jFerzl;

import java.io.*;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

public class InsuCsvProcessor {

    public static void applyInsuranceData(Path insuCsvPath, List<Patient> patients) throws IOException {
        try (BufferedReader reader =  new BufferedReader(new InputStreamReader(new FileInputStream(insuCsvPath.toFile()), "windows-1251"))) {
            String line;
            boolean skipHeader = true;

            while ((line = reader.readLine()) != null) {
                if (skipHeader) {
                    skipHeader = false;
                    continue;
                }

                String[] tokens = line.replace("\"", "").split(";", -1);
                if (tokens.length != 38) continue; // defensive

                long reqId = Long.parseLong(tokens[0].trim());
                int npp = Integer.parseInt(tokens[2].trim());

                // Only use the first record per patient (NPP = 1)
                if (npp != 1) continue;

                LocalDate dateB = parseDate(tokens[9]);
                LocalDate dateE = parseDate(tokens[10]);
                String okato = tokens[21].trim();
                String insurName = tokens[23].trim();
                String insurfName = tokens[24].trim();
                String insurCode = tokens[27].trim();
                String insurfCode = tokens[28].trim();

                for (Patient p : patients) {
                    if (p.getId() == reqId) {
                        p.setDateB(dateB);
                        p.setDateE(dateE);
                        p.setOkato(okato);
                        p.setInsurName(insurName);
                        p.setInsurfName(insurfName);
                        p.setInsurCode(insurCode);
                        p.setInsurfCode(insurfCode);
                        break;
                    }
                }
            }
        }
    }

    private static LocalDate parseDate(String raw) {
        try {
            return raw == null || raw.isBlank() ? null : LocalDate.parse(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
